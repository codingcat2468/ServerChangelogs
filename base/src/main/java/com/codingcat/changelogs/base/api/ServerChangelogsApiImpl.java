package com.codingcat.changelogs.base.api;

import com.codingcat.changelogs.api.Changelog;
import com.codingcat.changelogs.api.ChangelogSubscription;
import com.codingcat.changelogs.api.ServerChangelogsApi;
import com.codingcat.changelogs.api.event.ChangelogEvent;
import com.codingcat.changelogs.base.ServerChangelogs;
import com.codingcat.changelogs.base.dialog.DialogPackets;
import com.codingcat.changelogs.base.dialog.ui.ChangelogDialog;
import com.codingcat.changelogs.platformapi.player.IPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class ServerChangelogsApiImpl extends ServerChangelogsApi {
    private final @NotNull ServerChangelogs plugin;
    private final @NotNull ChangelogEventDispatcher eventDispatcher;
    private final @NotNull ChangelogService changelogService;
    private final @NotNull AtomicBoolean enabled = new AtomicBoolean(false);

    public ServerChangelogsApiImpl(@NotNull ServerChangelogs plugin) {
        this.plugin = plugin;
        this.eventDispatcher = new ChangelogEventDispatcher((event, error) ->
                plugin.getPlatform().getComponentLogger().error(
                        "A ServerChangelogs API listener failed while handling {}",
                        event.getClass().getSimpleName(),
                        error
                ));
        this.changelogService = new ChangelogService(plugin::getChangelogStorage, this.eventDispatcher::dispatch);
    }

    public void enable() {
        if (!this.enabled.compareAndSet(false, true))
            throw new IllegalStateException("The ServerChangelogs API is already enabled");
        try {
            this.registerProvider();
        } catch (RuntimeException e) {
            this.enabled.set(false);
            throw e;
        }
    }

    public void disable() {
        if (!this.enabled.compareAndSet(true, false)) return;
        this.unregisterProvider();
        this.eventDispatcher.clear();
    }

    @Override
    public @NotNull Changelog createChangelog(@NotNull List<Component> lines) {
        this.ensureEnabled();
        return this.changelogService.create(Objects.requireNonNull(lines, "lines"), null);
    }

    @Override
    public @NotNull Changelog createChangelog(@NotNull List<Component> lines, @NotNull Component author) {
        this.ensureEnabled();
        return this.changelogService.create(
                Objects.requireNonNull(lines, "lines"),
                Objects.requireNonNull(author, "author")
        );
    }

    @Override
    public @NotNull List<Changelog> getChangelogs() {
        this.ensureEnabled();
        return this.changelogService.getAll();
    }

    @Override
    public @NotNull Optional<Changelog> getChangelog(int id) {
        this.ensureEnabled();
        return this.changelogService.get(id);
    }

    @Override
    public @NotNull Optional<Changelog> getLatestChangelog() {
        this.ensureEnabled();
        return this.changelogService.getLatest();
    }

    @Override
    public boolean hasRead(int changelogId, @NotNull UUID playerId) {
        this.ensureEnabled();
        return this.changelogService.hasRead(changelogId, Objects.requireNonNull(playerId, "playerId"));
    }

    @Override
    public boolean markAsRead(int changelogId, @NotNull UUID playerId) {
        this.ensureEnabled();
        return this.changelogService.markAsRead(changelogId, Objects.requireNonNull(playerId, "playerId"));
    }

    @Override
    public boolean openChangelogMenu(@NotNull UUID playerId) {
        this.ensureEnabled();
        IPlayer player = this.plugin.getPlatform().getPlayerManager()
                .getFromUUID(Objects.requireNonNull(playerId, "playerId"));
        if (player == null) return false;
        this.plugin.getDialogHolder().getFromType(ChangelogDialog.class)
                .showTo(player, this.plugin.getDialogSessionManager(), DialogPackets.PacketPhase.PLAY);
        return true;
    }

    @Override
    public <T extends ChangelogEvent> @NotNull ChangelogSubscription subscribe(
            @NotNull Class<T> eventType,
            @NotNull Consumer<? super T> listener
    ) {
        this.ensureEnabled();
        return this.eventDispatcher.subscribe(
                Objects.requireNonNull(eventType, "eventType"),
                Objects.requireNonNull(listener, "listener")
        );
    }

    private void ensureEnabled() {
        if (!this.enabled.get()) throw new IllegalStateException("ServerChangelogs is not enabled");
    }
}
