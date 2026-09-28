package com.codingcat.changelogs.api;

import com.codingcat.changelogs.api.event.ChangelogEvent;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerChangelogsApiTest {
    @Test
    void registersAndUnregistersProvider() {
        TestApi api = new TestApi();

        assertFalse(ServerChangelogsApi.isAvailable());
        assertThrows(IllegalStateException.class, ServerChangelogsApi::get);

        try {
            api.enable();
            assertTrue(ServerChangelogsApi.isAvailable());
            assertSame(api, ServerChangelogsApi.get());
            assertThrows(IllegalStateException.class, new TestApi()::enable);
        } finally {
            api.disable();
        }

        assertFalse(ServerChangelogsApi.isAvailable());
    }

    private static final class TestApi extends ServerChangelogsApi {
        void enable() {
            this.registerProvider();
        }

        void disable() {
            this.unregisterProvider();
        }

        @Override
        public Changelog createChangelog(List<Component> lines) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Changelog createChangelog(List<Component> lines, Component author) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Changelog> getChangelogs() {
            return List.of();
        }

        @Override
        public Optional<Changelog> getChangelog(int id) {
            return Optional.empty();
        }

        @Override
        public Optional<Changelog> getLatestChangelog() {
            return Optional.empty();
        }

        @Override
        public boolean hasRead(int changelogId, UUID playerId) {
            return false;
        }

        @Override
        public boolean markAsRead(int changelogId, UUID playerId) {
            return false;
        }

        @Override
        public boolean openChangelogMenu(UUID playerId) {
            return false;
        }

        @Override
        public <T extends ChangelogEvent> ChangelogSubscription subscribe(Class<T> eventType, Consumer<? super T> listener) {
            return () -> { };
        }
    }
}
