package com.codingcat.changelogs.api;

import net.kyori.adventure.text.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable snapshot of a published changelog.
 *
 * @param id          the changelog's current numeric identifier
 * @param lines       the lines displayed for this changelog
 * @param publishedAt the time at which the changelog was published
 * @param author      the displayed author, if one was supplied
 */
public record Changelog(int id, List<Component> lines, Instant publishedAt, Optional<Component> author) {
    /**
     * Creates an immutable changelog snapshot.
     */
    public Changelog {
        if (id < 0) throw new IllegalArgumentException("id must not be negative");
        lines = List.copyOf(lines);
        if (lines.isEmpty()) throw new IllegalArgumentException("lines must not be empty");
        if (lines.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("lines must not contain null values");
        Objects.requireNonNull(publishedAt, "publishedAt");
        Objects.requireNonNull(author, "author");
    }
}
