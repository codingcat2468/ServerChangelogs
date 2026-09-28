package com.codingcat.changelogs.api;

import com.codingcat.changelogs.api.event.ChangelogPublishedEvent;
import com.codingcat.changelogs.api.event.ChangelogReadEvent;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChangelogTest {
    @Test
    void defensivelyCopiesLines() {
        List<Component> lines = new ArrayList<>(List.of(Component.text("Original")));
        Changelog changelog = new Changelog(0, lines, Instant.EPOCH, Optional.empty());

        lines.set(0, Component.text("Changed"));

        assertEquals(List.of(Component.text("Original")), changelog.lines());
        assertThrows(UnsupportedOperationException.class, () -> changelog.lines().add(Component.empty()));
    }

    @Test
    void rejectsInvalidSnapshots() {
        List<Component> lines = List.of(Component.text("Changes"));

        assertThrows(IllegalArgumentException.class,
                () -> new Changelog(-1, lines, Instant.EPOCH, Optional.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> new Changelog(0, List.of(), Instant.EPOCH, Optional.empty()));
        assertThrows(NullPointerException.class,
                () -> new Changelog(0, null, Instant.EPOCH, Optional.empty()));
        assertThrows(NullPointerException.class,
                () -> new Changelog(0, java.util.Arrays.asList(Component.empty(), null), Instant.EPOCH, Optional.empty()));
        assertThrows(NullPointerException.class,
                () -> new Changelog(0, lines, null, Optional.empty()));
        assertThrows(NullPointerException.class,
                () -> new Changelog(0, lines, Instant.EPOCH, null));
    }

    @Test
    void rejectsInvalidEvents() {
        Changelog changelog = new Changelog(0, List.of(Component.text("Changes")), Instant.EPOCH, Optional.empty());

        assertThrows(NullPointerException.class, () -> new ChangelogPublishedEvent(null));
        assertThrows(NullPointerException.class, () -> new ChangelogReadEvent(null, UUID.randomUUID()));
        assertThrows(NullPointerException.class, () -> new ChangelogReadEvent(changelog, null));
    }
}
