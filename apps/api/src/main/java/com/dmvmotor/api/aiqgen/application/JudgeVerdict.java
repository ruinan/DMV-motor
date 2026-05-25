package com.dmvmotor.api.aiqgen.application;

import com.dmvmotor.api.aiqgen.application.DeepSeekChatClient.AiQGenException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parsed verdict from an LLM judge — every judge asks DeepSeek to return the
 * same JSON shape {@code {"pass": bool, "reason": "..."}} so this helper does
 * the parsing once.
 *
 * <p>If the LLM returns garbage, we treat that as a FAIL with the raw text in
 * the reason — defensive default avoids accepting questions when the judge
 * itself misbehaved.
 */
record JudgeVerdict(boolean pass, String reason) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static JudgeVerdict parse(String raw) {
        String json = stripCodeFence(raw).trim();
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (Exception e) {
            throw new AiQGenException("Judge output not JSON: " + truncate(raw, 200), e);
        }
        if (!root.isObject()) {
            throw new AiQGenException("Judge output not a JSON object: " + truncate(raw, 200));
        }
        JsonNode passNode = root.get("pass");
        JsonNode reasonNode = root.get("reason");
        boolean pass = passNode != null && passNode.isBoolean() && passNode.asBoolean();
        String reason = reasonNode == null || reasonNode.isNull() ? "" : reasonNode.asText();
        return new JudgeVerdict(pass, reason);
    }

    private static String stripCodeFence(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            t = (firstNl > 0) ? t.substring(firstNl + 1) : t.substring(3);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
