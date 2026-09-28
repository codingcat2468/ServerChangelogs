package com.codingcat.changelogs.api.event;

/**
 * A change emitted by the ServerChangelogs API.
 */
public sealed interface ChangelogEvent permits ChangelogPublishedEvent, ChangelogReadEvent {
}
