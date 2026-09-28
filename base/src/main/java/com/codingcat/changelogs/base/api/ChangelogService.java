package com.codingcat.changelogs.base.api;

import com.codingcat.changelogs.api.Changelog;
import com.codingcat.changelogs.api.event.ChangelogEvent;
import com.codingcat.changelogs.api.event.ChangelogPublishedEvent;
import com.codingcat.changelogs.api.event.ChangelogReadEvent;
import com.codingcat.changelogs.base.data.ChangelogEntry;
import com.codingcat.changelogs.base.data.ChangelogStorage;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class ChangelogService {
    private final @NotNull Supplier<ChangelogStorage> storageSupplier;
    private final @NotNull Consumer<ChangelogEvent> eventSink;

    ChangelogService(
            @NotNull Supplier<ChangelogStorage> storageSupplier,
            @NotNull Consumer<ChangelogEvent> eventSink
    ) {
        this.storageSupplier = storageSupplier;
        this.eventSink = eventSink;
    }

    @NotNull Changelog create(@NotNull List<Component> lines, @Nullable Component author) {
        ChangelogStorage storage = this.storage();
        Changelog changelog = new Changelog(
                storage.nextUID(),
                lines,
                Instant.now(),
                Optional.ofNullable(author)
        );
        storage.storeEntry(new ChangelogEntry(
                changelog.id(),
                changelog.lines(),
                changelog.publishedAt(),
                changelog.author().orElse(null),
                new HashSet<>()
        ));
        this.eventSink.accept(new ChangelogPublishedEvent(changelog));
        return changelog;
    }

    @NotNull List<Changelog> getAll() {
        return this.storage().listEntries().stream()
                .map(ChangelogService::snapshot)
                .toList();
    }

    @NotNull Optional<Changelog> get(int id) {
        return Optional.ofNullable(this.storage().getByUID(id))
                .map(ChangelogService::snapshot);
    }

    @NotNull Optional<Changelog> getLatest() {
        List<ChangelogEntry> entries = this.storage().listEntries();
        if (entries.isEmpty()) return Optional.empty();
        return Optional.of(snapshot(entries.getLast()));
    }

    boolean hasRead(int id, @NotNull UUID playerId) {
        ChangelogEntry entry = this.storage().getByUID(id);
        return entry != null && entry.playersRead().contains(playerId);
    }

    boolean markAsRead(int id, @NotNull UUID playerId) {
        ChangelogStorage storage = this.storage();
        ChangelogEntry entry = storage.getByUID(id);
        if (entry == null || entry.playersRead().contains(playerId)) return false;
        storage.markAsRead(id, playerId);
        this.eventSink.accept(new ChangelogReadEvent(snapshot(entry), playerId));
        return true;
    }

    private @NotNull ChangelogStorage storage() {
        return Objects.requireNonNull(this.storageSupplier.get(), "Changelog storage is not initialized");
    }

    private static @NotNull Changelog snapshot(@NotNull ChangelogEntry entry) {
        return new Changelog(
                entry.uid(),
                entry.lines(),
                entry.recordedAt(),
                Optional.ofNullable(entry.author())
        );
    }
}
