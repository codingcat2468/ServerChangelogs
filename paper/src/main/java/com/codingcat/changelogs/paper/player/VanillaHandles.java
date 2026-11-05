package com.codingcat.changelogs.paper.player;

import io.papermc.paper.connection.PlayerConfigurationConnection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/** Bridges Paper API objects to the vanilla ones the shared dialog layer talks to. */
final class VanillaHandles {
    // shortcut: PaperCommonConnection exposes its vanilla listener only through a protected field, replace with a getter if Paper ever adds one
    private static final @NotNull Field LISTENER_FIELD;

    static {
        try {
            LISTENER_FIELD = Class.forName("io.papermc.paper.connection.PaperCommonConnection").getDeclaredField("packetListener");
            LISTENER_FIELD.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private VanillaHandles() {
    }

    static @NotNull ServerPlayer of(@NotNull Player player) {
        return ((CraftPlayer) player).getHandle();
    }

    static @NotNull ServerConfigurationPacketListenerImpl of(@NotNull PlayerConfigurationConnection connection) {
        try {
            return (ServerConfigurationPacketListenerImpl) LISTENER_FIELD.get(connection);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access the vanilla listener of " + connection, e);
        }
    }
}
