package com.codingcat.changelogs.paper.player;

import com.codingcat.changelogs.platformapi.player.IPlayer;
import com.codingcat.changelogs.platformapi.player.IPlayerManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PaperPlayerManager implements IPlayerManager {
    private final @NotNull Server server;

    @Override
    public @NotNull Set<IPlayer> getOnlinePlayers() {
        return this.server.getOnlinePlayers()
                .stream().map(this::fromNative)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public @Nullable IPlayer getFromUUID(@NotNull UUID uuid) {
        return Optional.ofNullable(this.server.getPlayer(uuid))
                .map(this::fromNative).orElse(null);
    }

    @Override
    public @Nullable IPlayer getFromName(@NotNull String name) {
        return Optional.ofNullable(this.server.getPlayer(name))
                .map(this::fromNative).orElse(null);
    }

    @Override
    public @NotNull IPlayer fromNative(@NotNull Object nativePlayer) throws IllegalArgumentException {
        if (!(nativePlayer instanceof Player player))
            throw new IllegalArgumentException("Expected bukkit Player but got " + nativePlayer);
        return new PaperPlayerWrapper(player);
    }
}
