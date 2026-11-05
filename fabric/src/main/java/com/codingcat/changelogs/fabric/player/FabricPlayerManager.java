package com.codingcat.changelogs.fabric.player;

import com.codingcat.changelogs.platformapi.player.IPlayer;
import com.codingcat.changelogs.platformapi.player.IPlayerManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class FabricPlayerManager implements IPlayerManager {
    private volatile @Nullable MinecraftServer server;

    public void setServer(@NotNull MinecraftServer server) {
        this.server = server;
    }

    public @Nullable MinecraftServer getServer() {
        return this.server;
    }

    @Override
    public @NotNull Set<IPlayer> getOnlinePlayers() {
        MinecraftServer s = this.server;
        if (s == null) return Set.of();
        return s.getPlayerList().getPlayers().stream().map(this::fromNative).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public @Nullable IPlayer getFromUUID(@NotNull UUID uuid) {
        MinecraftServer s = this.server;
        ServerPlayer p = s != null ? s.getPlayerList().getPlayer(uuid) : null;
        return p != null ? this.fromNative(p) : null;
    }

    @Override
    public @Nullable IPlayer getFromName(@NotNull String name) {
        MinecraftServer s = this.server;
        ServerPlayer p = s != null ? s.getPlayerList().getPlayerByName(name) : null;
        return p != null ? this.fromNative(p) : null;
    }

    @Override
    public @NotNull IPlayer fromNative(@NotNull Object nativePlayer) throws IllegalArgumentException {
        if (!(nativePlayer instanceof ServerPlayer player))
            throw new IllegalArgumentException("Expected ServerPlayer but got " + nativePlayer);
        return new FabricPlayerWrapper(player);
    }
}
