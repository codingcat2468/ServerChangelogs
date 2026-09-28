package com.codingcat.changelogs.api.event;

import com.codingcat.changelogs.api.Changelog;

import java.util.Objects;
import java.util.UUID;

/**
 * Fired synchronously when a changelog changes from unread to read for a player.
 *
 * @param changelog the changelog which was read
 * @param playerId  the reader's UUID
 */
public record ChangelogReadEvent(Changelog changelog, UUID playerId) implements ChangelogEvent {
    /**
     * Creates a read event.
     */
    public ChangelogReadEvent {
        Objects.requireNonNull(changelog, "changelog");
        Objects.requireNonNull(playerId, "playerId");
    }
}
