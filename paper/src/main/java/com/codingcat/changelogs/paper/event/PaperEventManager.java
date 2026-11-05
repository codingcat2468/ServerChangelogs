package com.codingcat.changelogs.paper.event;

import com.codingcat.changelogs.paper.player.PaperPlayerManager;
import com.codingcat.changelogs.platformapi.event.IEventManager;
import com.codingcat.changelogs.platformapi.event.impl.IEvent;
import com.codingcat.changelogs.platformapi.event.util.PlatformEventMappings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class PaperEventManager implements IEventManager {
    private final @NotNull Plugin plugin;
    private final @NotNull PluginManager pluginManager;
    private final @NotNull Set<PlatformMappingListener<?>> listenerSet = new HashSet<>();
    private @Nullable PaperEventMappings eventMappings;

    public void initMappings(@NotNull PaperPlayerManager playerManager) {
        this.eventMappings = new PaperEventMappings(playerManager);
    }

    @Override
    public <T extends IEvent> void registerListener(@NotNull Class<T> eventCls, @NotNull Consumer<T> listener) {
        PlatformEventMappings.Mapping<Event, T, ?> mapping = Objects.requireNonNull(eventMappings, "Event mappings not initialized").findForPlatformEvent(eventCls);
        PlatformMappingListener<T> mappingListener = new PlatformMappingListener<>(listener, mapping);
        this.pluginManager.registerEvent(mapping.nativeEventCls(), mappingListener, EventPriority.NORMAL, mappingListener, plugin);
        this.listenerSet.add(mappingListener);
    }

    @Override
    public void unregisterListener(@NotNull Consumer<?> listener) {
        this.unregisterIf(l -> l.equals(listener));
    }

    @Override
    public void unregisterIf(@NotNull Predicate<Consumer<?>> listenerPredicate) {
        Predicate<PlatformMappingListener<?>> predicate = l -> listenerPredicate.test(l.listener);
        this.listenerSet.stream()
                .filter(predicate)
                .forEach(HandlerList::unregisterAll);
        this.listenerSet.removeIf(predicate);
    }

    @Override
    public void unregisterAll() {
        this.listenerSet.forEach(HandlerList::unregisterAll);
        this.listenerSet.clear();
    }

    @RequiredArgsConstructor
    private static final class PlatformMappingListener<T extends IEvent> implements Listener, EventExecutor {
        private final @Getter Consumer<T> listener;
        private final @NotNull PlatformEventMappings.Mapping<Event, T, ?> mapping;

        @Override
        public void execute(@NotNull Listener listener, @NotNull Event event) throws EventException {
            try {
                //noinspection unchecked
                T platformEvent = ((PlatformEventMappings.Mapping<Event, T, Event>) mapping).convertToPlatform(event);
                this.listener.accept(platformEvent);
            } catch (Throwable e) {
                throw new EventException(e, "Failed to handle event " + event + " (platform " + mapping.platformEventCls() + ")");
            }
        }
    }
}
