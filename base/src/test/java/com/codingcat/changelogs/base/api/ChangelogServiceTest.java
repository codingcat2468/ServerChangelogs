package com.codingcat.changelogs.base.api;

import com.codingcat.changelogs.api.Changelog;
import com.codingcat.changelogs.api.event.ChangelogEvent;
import com.codingcat.changelogs.api.event.ChangelogPublishedEvent;
import com.codingcat.changelogs.api.event.ChangelogReadEvent;
import com.codingcat.changelogs.base.data.ChangelogEntry;
import com.codingcat.changelogs.base.data.ChangelogStorage;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangelogServiceTest {
    @Test
    void publishesImmutableSnapshotsAndReportsLatest() {
        InMemoryStorage storage = new InMemoryStorage();
        List<ChangelogEvent> events = new ArrayList<>();
        ChangelogService service = new ChangelogService(() -> storage, events::add);

        Changelog first = service.create(List.of(Component.text("First")), null);
        Changelog second = service.create(List.of(Component.text("Second")), Component.text("Author"));

        assertEquals(0, first.id());
        assertEquals(second, service.getLatest().orElseThrow());
        assertEquals(List.of(first, second), service.getAll());
        assertEquals(Component.text("Author"), second.author().orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> first.lines().add(Component.empty()));
        assertEquals(2, events.size());
        assertInstanceOf(ChangelogPublishedEvent.class, events.getFirst());
    }

    @Test
    void emitsReadEventOnlyForFirstRead() {
        InMemoryStorage storage = new InMemoryStorage();
        List<ChangelogEvent> events = new ArrayList<>();
        ChangelogService service = new ChangelogService(() -> storage, events::add);
        Changelog changelog = service.create(List.of(Component.text("Changes")), null);
        UUID playerId = UUID.randomUUID();
        events.clear();

        assertFalse(service.hasRead(changelog.id(), playerId));
        assertTrue(service.markAsRead(changelog.id(), playerId));
        assertTrue(service.hasRead(changelog.id(), playerId));
        assertFalse(service.markAsRead(changelog.id(), playerId));
        assertFalse(service.markAsRead(99, playerId));

        assertEquals(1, events.size());
        ChangelogReadEvent event = assertInstanceOf(ChangelogReadEvent.class, events.getFirst());
        assertEquals(changelog, event.changelog());
        assertEquals(playerId, event.playerId());
    }

    private static final class InMemoryStorage implements ChangelogStorage {
        private final List<ChangelogEntry> entries = new ArrayList<>();

        @Override
        public void init() {
        }

        @Override
        public void shutdown() {
        }

        @Override
        public void storeEntry(ChangelogEntry entry) {
            this.entries.add(entry.uid(), entry);
        }

        @Override
        public void updateEntry(ChangelogEntry entry) {
            this.entries.set(entry.uid(), entry);
        }

        @Override
        public boolean removeEntry(int uid) {
            return false;
        }

        @Override
        public List<ChangelogEntry> listEntries() {
            return this.entries;
        }

        @Override
        public ChangelogEntry getByUID(int uid) {
            return uid >= 0 && uid < this.entries.size() ? this.entries.get(uid) : null;
        }

        @Override
        public void markAsRead(int uid, UUID player) {
            this.entries.get(uid).playersRead().add(player);
        }

        @Override
        public Instant getFirstSeenAt(UUID player) {
            return null;
        }

        @Override
        public Instant recordFirstSeen(UUID player, Instant seenAt) {
            return seenAt;
        }

        @Override
        public int nextUID() {
            return this.entries.size();
        }

        @Override
        public String getDisplayName() {
            return "In-memory";
        }
    }
}
