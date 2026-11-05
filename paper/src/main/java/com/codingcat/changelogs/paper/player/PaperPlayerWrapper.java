package com.codingcat.changelogs.paper.player;

import com.codingcat.changelogs.platformapi.player.IPlayer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

@RequiredArgsConstructor
public class PaperPlayerWrapper implements IPlayer {
    private final @NotNull Player player;

    @Override
    public @NotNull UUID getUniqueId() {
        return this.player.getUniqueId();
    }

    @Override
    public @NotNull Locale getClientLocale() {
        return this.player.locale();
    }

    @Override
    public boolean isFirstJoin() {
        return !this.player.hasPlayedBefore();
    }

    @Override
    public @NotNull TriState isNativeAdmin() {
        return TriState.byBoolean(player.isOp());
    }

    @Override
    public void kick(@Nullable Component reason) {
        this.player.kick(reason, PlayerKickEvent.Cause.PLUGIN);
    }

    @Override
    public @NotNull Audience asAudience() {
        return this.player;
    }

    @Override
    public @NotNull PermissionChecker asPermissionChecker() {
        return this.player::permissionValue;
    }

    @Override
    public @NotNull Object unwrapNative() {
        // The shared dialog layer sends vanilla packets, so it needs the vanilla player
        return VanillaHandles.of(this.player);
    }
}
