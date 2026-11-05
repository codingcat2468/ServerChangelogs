package com.codingcat.changelogs.base.data;

import com.codingcat.changelogs.base.data.storage.YamlChangelogStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlChangelogStorageTest {
    @Test
    void firstSeenSurvivesRestartWithoutOtherWrites(@TempDir Path dir) {
        Path file = dir.resolve("_data.yml");
        UUID player = UUID.randomUUID();
        Instant firstSeen = Instant.parse("2026-01-01T00:00:00Z");

        YamlChangelogStorage before = new YamlChangelogStorage(file);
        before.init();
        // Nothing else touches the storage afterwards, so this write alone has to reach the disk
        before.recordFirstSeen(player, firstSeen);

        YamlChangelogStorage after = new YamlChangelogStorage(file);
        after.init();
        assertEquals(firstSeen, after.getFirstSeenAt(player));
        // A later join keeps the original timestamp
        assertEquals(firstSeen, after.recordFirstSeen(player, firstSeen.plusSeconds(60)));
    }
}
