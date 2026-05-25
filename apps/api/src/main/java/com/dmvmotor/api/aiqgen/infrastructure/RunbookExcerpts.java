package com.dmvmotor.api.aiqgen.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code docs/dmv-m1-handbook.md} from disk and extracts a per-sub-topic
 * excerpt for the AI Q-gen pipeline. Line ranges are hand-curated against the
 * 1661-line handbook to give the fact-checker focused context (~ tens to a
 * couple hundred lines per sub-topic).
 *
 * <p>Single-instance reader: load the handbook once, slice on demand. Designed
 * to be invoked from the CLI runner (working dir = repo root).
 */
public final class RunbookExcerpts {

    private static final Path DEFAULT_HANDBOOK_PATH = Path.of("docs/dmv-m1-handbook.md");

    /**
     * Inclusive 1-based line ranges in {@code docs/dmv-m1-handbook.md} that
     * give each sub-topic a focused but sufficient context window.
     */
    private static final Map<String, int[]> RANGES = Map.ofEntries(
            Map.entry("TRAFFIC_REGULATORY_SIGNS",  new int[]{807, 845}),
            Map.entry("TRAFFIC_LANE_MARKINGS",     new int[]{629, 670}),
            Map.entry("ROW_INTERSECTIONS",         new int[]{807, 832}),
            Map.entry("ROW_VULNERABLE_USERS",      new int[]{833, 870}),
            Map.entry("SPEED_FOLLOWING_DISTANCE",  new int[]{679, 730}),
            Map.entry("SPEED_LIMITS_PASSING",      new int[]{725, 770}),
            Map.entry("LANE_POSITION_SELECTION",   new int[]{629, 680}),
            Map.entry("LANE_SPLITTING_SHARING",    new int[]{735, 770}),
            Map.entry("TURNING_CURVES",            new int[]{973, 1011}),
            Map.entry("MANEUVERS_SWERVE_BRAKE",    new int[]{945, 975}),
            Map.entry("ALCOHOL_BAC_LAW",           new int[]{1281, 1591}),
            Map.entry("DRUGS_IMPAIRMENT",          new int[]{1565, 1595}),
            Map.entry("SURFACES_SLIPPERY_TRACKS",  new int[]{1011, 1063}),
            Map.entry("EMERGENCIES_MECHANICAL",    new int[]{1063, 1140}),
            Map.entry("BASICS_PPE",                new int[]{369, 460}),
            Map.entry("BASICS_CONTROL_INSPECTION", new int[]{325, 595})
    );

    private final List<String> lines;

    public RunbookExcerpts() throws IOException {
        this(DEFAULT_HANDBOOK_PATH);
    }

    public RunbookExcerpts(Path handbookPath) throws IOException {
        this.lines = Files.readAllLines(handbookPath);
    }

    public String forSubTopic(String code) {
        int[] range = RANGES.get(code);
        if (range == null) {
            throw new IllegalArgumentException("No runbook excerpt range defined for sub-topic: " + code);
        }
        int start = Math.max(1, range[0]) - 1;
        int end = Math.min(lines.size(), range[1]);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            String line = lines.get(i);
            // Skip PDF-conversion artifacts that don't carry content.
            if (line.contains("<!-- PAGE_BREAK -->")) continue;
            sb.append(line).append('\n');
        }
        return sb.toString().trim();
    }

    public static boolean hasRangeFor(String code) {
        return RANGES.containsKey(code);
    }
}
