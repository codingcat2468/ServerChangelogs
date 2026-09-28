package com.codingcat.changelogs.api.event;

import com.codingcat.changelogs.api.Changelog;

import java.util.Objects;

/**
 * Fired synchronously after a changelog has been persisted.
 *
 * @param changelog the published changelog
 */
public record ChangelogPublishedEvent(Changelog changelog) implements ChangelogEvent {
    /**
     * Creates a published event.
     */
    public ChangelogPublishedEvent {
        Objects.requireNonNull(changelog, "changelog");
    }
}
