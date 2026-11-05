package com.codingcat.changelogs.fabric.event;

import com.codingcat.changelogs.fabric.player.FabricConfigurationPhaseWrapper;
import com.codingcat.changelogs.fabric.player.FabricPlayerManager;
import com.codingcat.changelogs.platformapi.event.IEventManager;
import com.codingcat.changelogs.platformapi.event.impl.IEvent;
import com.codingcat.changelogs.platformapi.event.impl.PlayerEnterConfigurationPhaseEvent;
import com.codingcat.changelogs.platformapi.event.impl.PlayerJoinEvent;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class FabricEventManager implements IEventManager {
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger("server_changelogs");
    private static final ConfigurationTask.Type TASK_TYPE = new ConfigurationTask.Type("server_changelogs:configuration_event");

    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();
    // Configuration listeners may block (the dialog freeze), so they must never run on the server thread
    private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final @NotNull FabricPlayerManager playerManager;

    @Override
    public <T extends IEvent> void registerListener(@NotNull Class<T> eventCls, @NotNull Consumer<T> listener) {
        this.listeners.computeIfAbsent(eventCls, _ -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void unregisterListener(@NotNull Consumer<?> listener) {
        this.unregisterIf(l -> l.equals(listener));
    }

    @Override
    public void unregisterIf(@NotNull Predicate<Consumer<?>> listenerPredicate) {
        this.listeners.values().forEach(l -> l.removeIf(listenerPredicate));
    }

    @Override
    public void unregisterAll() {
        this.listeners.clear();
    }

    @SuppressWarnings("unchecked")
    private <T extends IEvent> void dispatch(@NotNull Class<T> eventCls, @NotNull T event) {
        for (Consumer<?> listener : this.listeners.getOrDefault(eventCls, List.of()))
            ((Consumer<T>) listener).accept(event);
    }

    private boolean hasListeners(@NotNull Class<? extends IEvent> eventCls) {
        return !this.listeners.getOrDefault(eventCls, List.of()).isEmpty();
    }

    public void registerHooks() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                this.dispatch(PlayerJoinEvent.class, new PlayerJoinEvent(this.playerManager.fromNative(handler.getPlayer()), true)));
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (this.hasListeners(PlayerEnterConfigurationPhaseEvent.class))
                handler.addTask(new DispatchTask(handler, server));
        });
    }

    /**
     * Keeps the player in the configuration phase until all listeners of {@link PlayerEnterConfigurationPhaseEvent}
     * have returned, mirroring Paper's async configure event.
     */
    private final class DispatchTask implements ConfigurationTask {
        private final @NotNull ServerConfigurationPacketListenerImpl handler;
        private final @NotNull MinecraftServer server;

        private DispatchTask(@NotNull ServerConfigurationPacketListenerImpl handler, @NotNull MinecraftServer server) {
            this.handler = handler;
            this.server = server;
        }

        @Override
        public void start(@NotNull Consumer<Packet<?>> packetSender) {
            // shortcut: a player disconnecting while frozen leaves this thread parked, upgrade by cancelling the freeze future on disconnect
            CompletableFuture.runAsync(() -> dispatch(PlayerEnterConfigurationPhaseEvent.class,
                            new PlayerEnterConfigurationPhaseEvent(new FabricConfigurationPhaseWrapper(handler, server), true)), asyncExecutor)
                    .whenComplete((_, err) -> {
                        if (err != null) LOGGER.error("Failed to handle configuration phase event", err);
                        this.server.execute(() -> this.handler.completeTask(TASK_TYPE));
                    });
        }

        @Override
        public @NotNull Type type() {
            return TASK_TYPE;
        }
    }
}
