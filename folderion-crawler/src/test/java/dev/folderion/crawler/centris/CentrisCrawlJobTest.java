package dev.folderion.crawler.centris;

import dev.folderion.core.CommitResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CentrisCrawlJobTest {

    @Test
    void formatCommitStatus_appendsRawCacheStatus() {
        assertEquals(
                "UNCHANGED(hit)",
                CentrisCrawlJob.formatCommitStatus(CommitResult.Status.UNCHANGED, "hit"));
        assertEquals(
                "UNCHANGED(hit_validated)",
                CentrisCrawlJob.formatCommitStatus(CommitResult.Status.UNCHANGED, "hit_validated"));
        assertEquals(
                "UPDATED(miss)",
                CentrisCrawlJob.formatCommitStatus(CommitResult.Status.UPDATED, "miss"));
        assertEquals(
                "CREATED",
                CentrisCrawlJob.formatCommitStatus(CommitResult.Status.CREATED, null));
        assertEquals(
                "CREATED",
                CentrisCrawlJob.formatCommitStatus(CommitResult.Status.CREATED, "  "));
    }
}
