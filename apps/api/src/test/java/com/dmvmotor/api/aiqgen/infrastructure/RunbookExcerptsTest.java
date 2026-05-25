package com.dmvmotor.api.aiqgen.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RunbookExcerptsTest {

    @Test
    void forSubTopic_returnsLinesInRange(@TempDir Path dir) throws IOException {
        Path handbook = dir.resolve("hb.md");
        List<String> lines = IntStream.rangeClosed(1, 1700)
                .mapToObj(i -> "L" + i + " content")
                .toList();
        Files.write(handbook, lines);

        RunbookExcerpts excerpts = new RunbookExcerpts(handbook);
        String laneSplitting = excerpts.forSubTopic("LANE_SPLITTING_SHARING");

        // Range 735-770 → first line should be L735, last should include L770.
        assertThat(laneSplitting).contains("L735 content");
        assertThat(laneSplitting).contains("L770 content");
        assertThat(laneSplitting).doesNotContain("L734 content");
        assertThat(laneSplitting).doesNotContain("L771 content");
    }

    @Test
    void forSubTopic_stripsPageBreakArtifacts(@TempDir Path dir) throws IOException {
        Path handbook = dir.resolve("hb.md");
        // Build content where lines 1..50 include 2 page-break markers.
        List<String> lines = IntStream.rangeClosed(1, 1700)
                .mapToObj(i -> i == 740 || i == 750 ? "<!-- PAGE_BREAK -->" : "L" + i)
                .toList();
        Files.write(handbook, lines);

        RunbookExcerpts excerpts = new RunbookExcerpts(handbook);
        String text = excerpts.forSubTopic("LANE_SPLITTING_SHARING");
        assertThat(text).doesNotContain("PAGE_BREAK");
        assertThat(text).contains("L739");
        assertThat(text).contains("L741");
    }

    @Test
    void forSubTopic_unknownCode_throws(@TempDir Path dir) throws IOException {
        Path handbook = dir.resolve("hb.md");
        Files.write(handbook, List.of("anything"));
        RunbookExcerpts excerpts = new RunbookExcerpts(handbook);
        assertThatThrownBy(() -> excerpts.forSubTopic("NOPE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NOPE");
    }

    @Test
    void hasRangeFor_returnsTrueForKnownCodes() {
        assertThat(RunbookExcerpts.hasRangeFor("LANE_SPLITTING_SHARING")).isTrue();
        assertThat(RunbookExcerpts.hasRangeFor("EMERGENCIES_MECHANICAL")).isTrue();
        assertThat(RunbookExcerpts.hasRangeFor("MANEUVERS_SWERVE_BRAKE")).isTrue();
        assertThat(RunbookExcerpts.hasRangeFor("UNKNOWN")).isFalse();
    }

    @Test
    void allSixteenSubTopicsHaveRanges() {
        String[] codes = {
                "TRAFFIC_REGULATORY_SIGNS", "TRAFFIC_LANE_MARKINGS",
                "ROW_INTERSECTIONS", "ROW_VULNERABLE_USERS",
                "SPEED_FOLLOWING_DISTANCE", "SPEED_LIMITS_PASSING",
                "LANE_POSITION_SELECTION", "LANE_SPLITTING_SHARING",
                "TURNING_CURVES", "MANEUVERS_SWERVE_BRAKE",
                "ALCOHOL_BAC_LAW", "DRUGS_IMPAIRMENT",
                "SURFACES_SLIPPERY_TRACKS", "EMERGENCIES_MECHANICAL",
                "BASICS_PPE", "BASICS_CONTROL_INSPECTION"
        };
        for (String code : codes) {
            assertThat(RunbookExcerpts.hasRangeFor(code))
                    .as("range for %s", code).isTrue();
        }
    }

    @Test
    void realHandbook_loadsAndReturnsLaneSplittingExcerpt() throws IOException {
        // Sanity check against the actual vendored handbook.
        Path realHandbook = Path.of("..", "..", "docs", "dmv-m1-handbook.md");
        // Test runs from apps/api, so handbook is two levels up
        if (!Files.exists(realHandbook)) {
            // Fall back to project-root-relative for IDE runs
            realHandbook = Path.of("docs", "dmv-m1-handbook.md");
        }
        if (!Files.exists(realHandbook)) return; // skip if can't locate

        RunbookExcerpts excerpts = new RunbookExcerpts(realHandbook);
        String text = excerpts.forSubTopic("LANE_SPLITTING_SHARING");
        assertThat(text.toLowerCase()).contains("lane splitting");
    }
}
