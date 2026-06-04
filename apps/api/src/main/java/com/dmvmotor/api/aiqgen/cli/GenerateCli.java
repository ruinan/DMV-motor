package com.dmvmotor.api.aiqgen.cli;

import com.dmvmotor.api.aisupport.config.AiProperties;
import com.dmvmotor.api.aiqgen.application.CoverageJudge;
import com.dmvmotor.api.aiqgen.application.DeepSeekChatClient;
import com.dmvmotor.api.aiqgen.application.DifficultyJudge;
import com.dmvmotor.api.aiqgen.application.FormatValidator;
import com.dmvmotor.api.aiqgen.application.GenerationOrchestrator;
import com.dmvmotor.api.aiqgen.application.QuestionGenerator;
import com.dmvmotor.api.aiqgen.application.RunbookFactChecker;
import com.dmvmotor.api.aiqgen.domain.GeneratedQuestion;
import com.dmvmotor.api.aiqgen.domain.SubTopicSpec;
import com.dmvmotor.api.aiqgen.infrastructure.RunbookExcerpts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Out-of-band CLI that drives the AI question generation pipeline for one or
 * more thin sub-topics and emits a V16 Flyway migration to stdout.
 *
 * <p>Usage:
 * <pre>
 *   APP_AI_DEEPSEEK_API_KEY=sk-... \
 *   java -cp apps/api/target/classes:... com.dmvmotor.api.aiqgen.cli.GenerateCli \
 *        LANE_SPLITTING_SHARING:9 MANEUVERS_SWERVE_BRAKE:9 EMERGENCIES_MECHANICAL:10 \
 *        &gt; apps/api/src/main/resources/db/migration/V16__ai_generated_questions.sql
 * </pre>
 *
 * <p>Reads sub-topic identity (name_en/name_zh/description) from the local
 * Postgres so generated rows match the catalog exactly. Reads each sub-topic's
 * handbook excerpt from the transient, gitignored cache (see {@link RunbookExcerpts})
 * — the source handbook is intentionally never vendored into the repo (copyright).
 */
public final class GenerateCli {

    private GenerateCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: GenerateCli SUB_TOPIC_CODE:COUNT [SUB_TOPIC_CODE:COUNT ...]");
            System.exit(2);
        }

        List<Request> requests = new ArrayList<>();
        for (String arg : args) {
            int colon = arg.indexOf(':');
            if (colon <= 0 || colon == arg.length() - 1) {
                System.err.println("Bad arg (expected CODE:COUNT): " + arg);
                System.exit(2);
            }
            String code = arg.substring(0, colon);
            int count = Integer.parseInt(arg.substring(colon + 1));
            requests.add(new Request(code, count));
        }

        String apiKey = require("APP_AI_DEEPSEEK_API_KEY");
        // max_tokens=4000 covers ~5 bilingual MCQs per batch (each ~600 tokens).
        // Timeout 180s — DeepSeek can take 30-60s for a 4-question batch.
        AiProperties props = new AiProperties(
                true, "deepseek", 120, 60, 300, 50, 10,
                new AiProperties.Deepseek(apiKey, "https://api.deepseek.com", "deepseek-chat", 4000, 180));

        DeepSeekChatClient client = new DeepSeekChatClient(props);
        QuestionGenerator generator = new QuestionGenerator(client);
        FormatValidator formatValidator = new FormatValidator();
        CoverageJudge coverageJudge = new CoverageJudge(client);
        DifficultyJudge difficultyJudge = new DifficultyJudge(client);
        RunbookFactChecker factChecker = new RunbookFactChecker(client);
        GenerationOrchestrator orchestrator = new GenerationOrchestrator(
                generator, formatValidator, coverageJudge, difficultyJudge, factChecker);
        RunbookExcerpts excerpts = new RunbookExcerpts();

        String dbUrl = System.getenv().getOrDefault("PG_URL", "jdbc:postgresql://localhost:5432/dmv_motor");
        String dbUser = System.getenv().getOrDefault("PGUSER", "dmv_motor");
        String dbPass = System.getenv().getOrDefault("PGPASSWORD", "dmv_motor");

        StringBuilder sql = new StringBuilder();
        sql.append("-- AI-generated questions (aiqgen pipeline, DeepSeek-chat).\n");
        sql.append("-- Generation control loop: 4 gates (format / coverage / difficulty / runbook fact-check).\n");
        sql.append("-- Grounding excerpts fetched transiently from the official handbook (not vendored).\n\n");

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            for (Request req : requests) {
                SubTopicSpec spec = loadSpec(conn, req.code(), excerpts);
                System.err.println("--- Generating " + req.count() + " questions for " + req.code());
                // Batch capped at 4 per call, so allow more retries for larger targets.
                int retryBudget = Math.max(3, req.count());
                GenerationOrchestrator.Result result = orchestrator.run(spec, req.count(), retryBudget);
                System.err.println("Accepted: " + result.accepted().size()
                        + " / target " + req.count()
                        + " after " + result.attemptsUsed() + " attempt(s)");
                if (!result.failureFeedback().isEmpty()) {
                    System.err.println("Sample failures:");
                    result.failureFeedback().stream().limit(3)
                            .forEach(f -> System.err.println("  - " + f));
                }
                sql.append(renderSqlBlock(req.code(), result.accepted()));
            }
        }

        // Write SQL as explicit UTF-8 to OUT_SQL (so bilingual ZH text survives the
        // Windows console codepage, and logback noise on stdout can't corrupt it).
        // Falls back to stdout if OUT_SQL is unset.
        String outPath = System.getenv("OUT_SQL");
        if (outPath != null && !outPath.isBlank()) {
            Files.writeString(Path.of(outPath), sql.toString(), StandardCharsets.UTF_8);
            System.err.println("Wrote SQL (" + sql.length() + " chars, UTF-8) to " + outPath);
        } else {
            System.out.print(sql);
        }
    }

    // --------- helpers ---------

    private record Request(String code, int count) {}

    private static String require(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) {
            System.err.println("Missing required env var: " + name);
            System.exit(2);
        }
        return v;
    }

    private static SubTopicSpec loadSpec(Connection conn, String code, RunbookExcerpts excerpts) throws SQLException, IOException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT code, name_en, name_zh, description FROM sub_topics WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Sub-topic not found in DB: " + code);
                }
                return new SubTopicSpec(
                        rs.getString("code"),
                        rs.getString("name_en"),
                        rs.getString("name_zh"),
                        rs.getString("description"),
                        excerpts.forSubTopic(code));
            }
        }
    }

    private static String renderSqlBlock(String subTopicCode, List<GeneratedQuestion> accepted) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n-- ============================================================\n");
        sb.append("-- Sub-topic: ").append(subTopicCode).append("  (").append(accepted.size()).append(" questions)\n");
        sb.append("-- ============================================================\n");
        sb.append("DO $$\n");
        sb.append("DECLARE\n");
        sb.append("    st_id      BIGINT;\n");
        sb.append("    parent_id  BIGINT;\n");
        sb.append("    v_exam_id  BIGINT;\n");
        sb.append("    new_q_id   BIGINT;\n");
        sb.append("BEGIN\n");
        sb.append("    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = '")
                .append(subTopicCode).append("';\n");
        // questions.exam_id is NOT NULL (V26): inherit it from the parent topic so
        // generated rows are scoped to the right exam (CA-M1, CA-C, ...).
        sb.append("    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;\n\n");
        int n = 0;
        for (GeneratedQuestion q : accepted) {
            n++;
            sb.append("    -- Q").append(n).append(": ").append(escapeComment(q.en().stem())).append('\n');
            sb.append("    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)\n");
            sb.append("    VALUES (parent_id, st_id, v_exam_id, '").append(q.correctChoiceKey())
                    .append("', 'active', false, true, true, true)\n");
            sb.append("    RETURNING id INTO new_q_id;\n");

            sb.append("    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)\n");
            sb.append("    VALUES (new_q_id, 'en', ").append(sqlString(q.en().stem())).append(", ")
                    .append(sqlString(renderChoicesJson(q.en().choices()))).append("::jsonb, ")
                    .append(sqlString(q.en().explanation())).append(", 'active');\n");

            sb.append("    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)\n");
            sb.append("    VALUES (new_q_id, 'zh', ").append(sqlString(q.zh().stem())).append(", ")
                    .append(sqlString(renderChoicesJson(q.zh().choices()))).append("::jsonb, ")
                    .append(sqlString(q.zh().explanation())).append(", 'active');\n\n");
        }
        sb.append("END $$;\n");
        return sb.toString();
    }

    private static String renderChoicesJson(List<GeneratedQuestion.Choice> choices) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < choices.size(); i++) {
            if (i > 0) sb.append(',');
            GeneratedQuestion.Choice c = choices.get(i);
            sb.append("{\"key\":\"").append(c.key()).append("\",\"text\":")
              .append(jsonString(c.text())).append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    private static String sqlString(String s) {
        if (s == null) return "NULL";
        return "'" + s.replace("'", "''") + "'";
    }

    private static String jsonString(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static String escapeComment(String s) {
        if (s == null) return "";
        String clean = s.replace("\n", " ").replace("\r", " ");
        return clean.length() > 100 ? clean.substring(0, 100) + "..." : clean;
    }
}
