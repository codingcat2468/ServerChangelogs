package com.codingcat.changelogs.api;

import com.codingcat.changelogs.api.event.ChangelogEvent;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * The public, platform-neutral ServerChangelogs API.
 *
 * <p>API calls are synchronous and the backing storage is not thread-safe. Call
 * all methods from the server's main thread unless the active platform explicitly
 * documents otherwise.</p>
 */
public abstract class ServerChangelogsApi {
    private static final AtomicReference<ServerChangelogsApi> INSTANCE = new AtomicReference<>();

    /**
     * Constructor for the ServerChangelogs-managed implementation.
     */
    protected ServerChangelogsApi() {
    }

    /**
     * Gets the API exposed by the running ServerChangelogs plugin.
     *
     * @return the active API instance
     * @throws IllegalStateException if ServerChangelogs has not finished enabling
     */
    public static ServerChangelogsApi get() {
        ServerChangelogsApi api = INSTANCE.get();
        if (api == null) throw new IllegalStateException("ServerChangelogs is not enabled");
        return api;
    }

    /**
     * Checks whether a ServerChangelogs API instance is currently available.
     *
     * @return {@code true} after ServerChangelogs has enabled
     */
    public static boolean isAvailable() {
        return INSTANCE.get() != null;
    }

    /**
     * Registers this implementation as the active API provider.
     * Intended only for the ServerChangelogs implementation.
     */
    protected final void registerProvider() {
        if (!INSTANCE.compareAndSet(null, this))
            throw new IllegalStateException("A ServerChangelogs API provider is already registered");
    }

    /**
     * Removes this implementation if it is the active API provider.
     * Intended only for the ServerChangelogs implementation.
     */
    protected final void unregisterProvider() {
        INSTANCE.compareAndSet(this, null);
    }

    /**
     * Publishes a changelog without a displayed author.
     *
     * @param lines the non-empty changelog contents
     * @return an immutable snapshot of the published changelog
     */
    public abstract Changelog createChangelog(List<Component> lines);

    /**
     * Publishes a changelog.
     *
     * @param lines  the non-empty changelog contents
     * @param author the displayed author
     * @return an immutable snapshot of the published changelog
     */
    public abstract Changelog createChangelog(List<Component> lines, Component author);

    /**
     * Gets all changelogs in publication order.
     *
     * @return immutable changelog snapshots
     */
    public abstract List<Changelog> getChangelogs();

    /**
     * Looks up a changelog by its current identifier.
     *
     * @param id the current changelog identifier
     * @return the changelog, or an empty optional if it does not exist
     */
    public abstract Optional<Changelog> getChangelog(int id);

    /**
     * Gets the most recently published changelog, if one exists.
     *
     * @return the latest changelog, or an empty optional when there are none
     */
    public abstract Optional<Changelog> getLatestChangelog();

    /**
     * Checks whether a player has read a changelog.
     * Returns {@code false} when the changelog does not exist.
     *
     * @param changelogId the current changelog identifier
     * @param playerId    the player's UUID
     * @return whether the changelog exists and has been read by the player
     */
    public abstract boolean hasRead(int changelogId, UUID playerId);

    /**
     * Marks a changelog as read by a player.
     *
     * @param changelogId the current changelog identifier
     * @param playerId    the player's UUID
     * @return {@code true} only when the changelog existed and changed from unread to read
     */
    public abstract boolean markAsRead(int changelogId, UUID playerId);

    /**
     * Opens the changelog menu for an online player.
     *
     * @param playerId the player's UUID
     * @return {@code false} if the player is not currently online
     */
    public abstract boolean openChangelogMenu(UUID playerId);

    /**
     * Subscribes to one exact type of changelog event. Listeners run synchronously
     * on the thread which caused the event.
     *
     * @param <T>       the event type
     * @param eventType the event class to receive
     * @param listener  the listener to invoke
     * @return a handle which can be closed to unsubscribe
     */
    public abstract <T extends ChangelogEvent> ChangelogSubscription subscribe(
            Class<T> eventType,
            Consumer<? super T> listener
    );

}
