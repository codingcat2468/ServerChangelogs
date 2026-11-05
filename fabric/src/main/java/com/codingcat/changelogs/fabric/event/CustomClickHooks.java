package com.codingcat.changelogs.fabric.event;

import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Static bridge between the mixin (which has no access to the platform instance) and the dialog session manager. */
public final class CustomClickHooks {
    @FunctionalInterface
    public interface Handler {
        void handle(@NotNull ServerCommonPacketListenerImpl listener, @NotNull MinecraftServer server, @NotNull ServerboundCustomClickActionPacket packet);
    }

    private static volatile @Nullable Handler handler;

    private CustomClickHooks() {
    }

    public static void setHandler(@Nullable Handler handler) {
        CustomClickHooks.handler = handler;
    }

    public static void dispatch(@NotNull ServerCommonPacketListenerImpl listener, @NotNull MinecraftServer server, @NotNull ServerboundCustomClickActionPacket packet) {
        Handler h = handler;
        if (h != null) h.handle(listener, server, packet);
    }
}
