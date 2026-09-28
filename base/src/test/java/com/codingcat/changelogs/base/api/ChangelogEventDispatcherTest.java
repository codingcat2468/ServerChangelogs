package com.codingcat.changelogs.base.api;

import com.codingcat.changelogs.api.Changelog;
import com.codingcat.changelogs.api.ChangelogSubscription;
import com.codingcat.changelogs.api.event.ChangelogEvent;
import com.codingcat.changelogs.api.event.ChangelogPublishedEvent;
import com.codingcat.changelogs.api.event.ChangelogReadEvent;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ChangelogEventDispatcherTest {
    @Test
    void dispatchesExactTypesAndSupportsIdempotentUnsubscribe() {
        List<ChangelogEvent> handled = new ArrayList<>();
        ChangelogEventDispatcher dispatcher = new ChangelogEventDispatcher((_, _) -> { });
        ChangelogSubscription subscription = dispatcher.subscribe(ChangelogPublishedEvent.class, handled::add);
        Changelog changelog = changelog();

        dispatcher.dispatch(new ChangelogReadEvent(changelog, UUID.randomUUID()));
        ChangelogPublishedEvent published = new ChangelogPublishedEvent(changelog);
        dispatcher.dispatch(published);
        subscription.unsubscribe();
        subscription.unsubscribe();
        dispatcher.dispatch(published);

        assertEquals(List.of(published), handled);
    }

    @Test
    void isolatesFailingListeners() {
        List<RuntimeException> failures = new ArrayList<>();
        List<ChangelogPublishedEvent> handled = new ArrayList<>();
        ChangelogEventDispatcher dispatcher = new ChangelogEventDispatcher((_, error) -> failures.add(error));
        RuntimeException failure = new RuntimeException("listener failed");
        dispatcher.subscribe(ChangelogPublishedEvent.class, _ -> { throw failure; });
        dispatcher.subscribe(ChangelogPublishedEvent.class, handled::add);
        ChangelogPublishedEvent event = new ChangelogPublishedEvent(changelog());

        dispatcher.dispatch(event);

        assertEquals(List.of(event), handled);
        assertEquals(1, failures.size());
        assertSame(failure, failures.getFirst());
    }

    private static Changelog changelog() {
        return new Changelog(0, List.of(Component.text("Changes")), Instant.EPOCH, Optional.empty());
    }
}
