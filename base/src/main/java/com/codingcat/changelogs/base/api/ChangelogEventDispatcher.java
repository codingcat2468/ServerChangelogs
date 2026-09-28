package com.codingcat.changelogs.base.api;

import com.codingcat.changelogs.api.ChangelogSubscription;
import com.codingcat.changelogs.api.event.ChangelogEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class ChangelogEventDispatcher {
    private final @NotNull Map<Class<? extends ChangelogEvent>, CopyOnWriteArrayList<Consumer<ChangelogEvent>>> listeners = new ConcurrentHashMap<>();
    private final @NotNull BiConsumer<ChangelogEvent, RuntimeException> exceptionHandler;

    ChangelogEventDispatcher(@NotNull BiConsumer<ChangelogEvent, RuntimeException> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    <T extends ChangelogEvent> @NotNull ChangelogSubscription subscribe(
            @NotNull Class<T> eventType,
            @NotNull Consumer<? super T> listener
    ) {
        Consumer<ChangelogEvent> adapter = event -> listener.accept(eventType.cast(event));
        CopyOnWriteArrayList<Consumer<ChangelogEvent>> eventListeners =
                this.listeners.computeIfAbsent(eventType, _ -> new CopyOnWriteArrayList<>());
        eventListeners.add(adapter);
        AtomicBoolean active = new AtomicBoolean(true);
        return () -> {
            if (!active.compareAndSet(true, false)) return;
            eventListeners.remove(adapter);
            if (eventListeners.isEmpty()) this.listeners.remove(eventType, eventListeners);
        };
    }

    void dispatch(@NotNull ChangelogEvent event) {
        CopyOnWriteArrayList<Consumer<ChangelogEvent>> eventListeners = this.listeners.get(event.getClass());
        if (eventListeners == null) return;
        for (Consumer<ChangelogEvent> listener : eventListeners) {
            try {
                listener.accept(event);
            } catch (RuntimeException e) {
                this.exceptionHandler.accept(event, e);
            }
        }
    }

    void clear() {
        this.listeners.clear();
    }
}
