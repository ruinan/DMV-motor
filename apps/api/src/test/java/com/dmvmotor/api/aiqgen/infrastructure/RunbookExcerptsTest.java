package com.dmvmotor.api.aiqgen.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RunbookExcerptsTest {

    @Test
    void forSubTopic_readsTrimmedCachedExcerptFile(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("CAC_TRAFFIC_SIGNALS.txt"),
                "  A solid red light means a full stop.\n");

        RunbookExcerpts excerpts = new RunbookExcerpts(dir);

        assertThat(excerpts.forSubTopic("CAC_TRAFFIC_SIGNALS"))
                .isEqualTo("A solid red light means a full stop.");
    }

    @Test
    void forSubTopic_missingFile_throwsWithGuidance(@TempDir Path dir) {
        RunbookExcerpts excerpts = new RunbookExcerpts(dir);

        assertThatThrownBy(() -> excerpts.forSubTopic("NOPE"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOPE")
                .hasMessageContaining("not vendored");
    }

    @Test
    void forSubTopic_emptyFile_throws(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("EMPTY.txt"), "   \n");
        RunbookExcerpts excerpts = new RunbookExcerpts(dir);

        assertThatThrownBy(() -> excerpts.forSubTopic("EMPTY"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void hasExcerptFor_reflectsCachePresence(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("PRESENT.txt"), "x");
        RunbookExcerpts excerpts = new RunbookExcerpts(dir);

        assertThat(excerpts.hasExcerptFor("PRESENT")).isTrue();
        assertThat(excerpts.hasExcerptFor("ABSENT")).isFalse();
    }
}
