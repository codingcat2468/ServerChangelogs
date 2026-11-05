package com.codingcat.changelogs.paper.player;

import com.codingcat.changelogs.platformapi.player.IPlayer;
import com.destroystokyo.paper.ClientOption;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import static net.kyori.adventure.text.Component.empty;

@RequiredArgsConstructor
@SuppressWarnings("UnstableApiUsage")
public class PaperConfigurationPhaseConnectionWrapper implements IPlayer {
    private final @NotNull PlayerConfigurationConnection connection;

    @Override
    public @NotNull UUID getUniqueId() {
        return Objects.requireNonNull(this.connection.getProfile().getId(), "Player UUID was null");
    }

    @Override
    public @NotNull Locale getClientLocale() {
        return Locale.of(this.connection.getClientOption(ClientOption.LOCALE));
    }

    @Override
    public boolean isFirstJoin() {
        return false;
    }

    @Override
    public @NotNull TriState isNativeAdmin() {
        return TriState.NOT_SET;
    }

    @Override
    public void kick(@Nullable Component reason) {
        this.connection.disconnect(reason != null ? reason : empty());
    }

    @Override
    public @NotNull Audience asAudience() {
        return this.connection.getAudience();
    }

    @Override
    public @NotNull PermissionChecker asPermissionChecker() {
        Player player = Bukkit.getPlayer(this.getUniqueId());
        return player != null ? player::permissionValue : PermissionChecker.always(TriState.NOT_SET);
    }

    @Override
    public @NotNull Object unwrapNative() {
        // The shared dialog layer sends vanilla packets, so it needs the vanilla listener
        return VanillaHandles.of(this.connection);
    }
}
