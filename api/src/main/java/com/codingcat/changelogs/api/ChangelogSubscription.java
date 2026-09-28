package com.codingcat.changelogs.api;

/**
 * A live subscription to ServerChangelogs events.
 */
@FunctionalInterface
public interface ChangelogSubscription extends AutoCloseable {
    /**
     * Stops this subscription. Calling this method more than once has no effect.
     */
    void unsubscribe();

    @Override
    default void close() {
        this.unsubscribe();
    }
}
