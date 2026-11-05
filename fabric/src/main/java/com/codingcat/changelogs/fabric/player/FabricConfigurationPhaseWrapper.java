package com.codingcat.changelogs.fabric.player;

import com.codingcat.changelogs.fabric.mixin.ServerConfigurationPacketListenerImplAccessor;
import com.codingcat.changelogs.platformapi.player.IPlayer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/** A player that is still in the configuration phase, so there is no ServerPlayer yet. */
@RequiredArgsConstructor
public class FabricConfigurationPhaseWrapper implements IPlayer {
    private final @NotNull ServerConfigurationPacketListenerImpl handler;
    private final @NotNull MinecraftServer server;

    @Override
    public @NotNull UUID getUniqueId() {
        return this.handler.getOwner().id();
    }

    @Override
    public @NotNull Locale getClientLocale() {
        var info = ((ServerConfigurationPacketListenerImplAccessor) this.handler).changelogs$getClientInformation();
        return FabricPlayerWrapper.parseLocale(info != null ? info.language() : null);
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
        this.handler.disconnect(MinecraftServerAudiences.of(this.server).asNative(reason != null ? reason : Component.empty()));
    }

    @Override
    public @NotNull Audience asAudience() {
        // No chat in the configuration phase
        return Audience.empty();
    }

    @Override
    public @NotNull PermissionChecker asPermissionChecker() {
        return PermissionChecker.always(TriState.NOT_SET);
    }

    @Override
    public @NotNull Object unwrapNative() {
        return this.handler;
    }
}
