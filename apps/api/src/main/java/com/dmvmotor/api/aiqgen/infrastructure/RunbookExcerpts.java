package com.dmvmotor.api.aiqgen.infrastructure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads a per-sub-topic handbook excerpt from a transient, <b>gitignored</b>
 * local cache directory (default {@code apps/api/handbook-cache}, overridable via
 * the {@code HANDBOOK_CACHE_DIR} env var). One UTF-8 text file per sub-topic
 * code: {@code <cacheDir>/<SUB_TOPIC_CODE>.txt}.
 *
 * <p><b>Copyright stance</b>: the source DMV handbook is never vendored into the
 * repo. A refresh step fetches the relevant section text transiently and drops it
 * into this cache; the AI Q-gen pipeline consumes it to generate <em>original</em>
 * questions; only those generated questions are persisted (as Flyway migrations).
 * Anchoring on per-sub-topic files (not line ranges into a committed handbook)
 * means a periodic refresh just overwrites the cache and nothing downstream
 * changes — and works for any exam (the code is the key, exams use distinct
 * prefixes, e.g. {@code CAC_*}).
 */
public final class RunbookExcerpts {

    /** Repo-relative default; gitignored. Run the CLI with working dir = repo root. */
    public static final String DEFAULT_CACHE_DIR = "apps/api/handbook-cache";

    private final Path cacheDir;

    public RunbookExcerpts() {
        this(Path.of(System.getenv().getOrDefault("HANDBOOK_CACHE_DIR", DEFAULT_CACHE_DIR)));
    }

    public RunbookExcerpts(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    /**
     * The cached handbook excerpt for a sub-topic. Throws (with guidance) if it
     * hasn't been fetched into the cache yet — generation must not silently run
     * without grounding text.
     */
    public String forSubTopic(String code) {
        Path file = cacheDir.resolve(code + ".txt");
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException(
                    "No handbook excerpt cached for sub-topic '" + code + "' at " + file
                    + ". Fetch the relevant handbook section into the cache first — "
                    + "the source handbook is intentionally not vendored (copyright).");
        }
        String text;
        try {
            text = Files.readString(file).trim();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading cached excerpt for '" + code + "': " + file, e);
        }
        if (text.isEmpty()) {
            throw new IllegalStateException("Cached excerpt for '" + code + "' is empty: " + file);
        }
        return text;
    }

    /** Whether an excerpt has been cached for this sub-topic code. */
    public boolean hasExcerptFor(String code) {
        return Files.isRegularFile(cacheDir.resolve(code + ".txt"));
    }
}
