package com.codingcat.changelogs.fabric.player;

import com.codingcat.changelogs.platformapi.player.IPlayer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

@RequiredArgsConstructor
public class FabricPlayerWrapper implements IPlayer {
    private final @NotNull ServerPlayer player;

    static @NotNull Locale parseLocale(@Nullable String clientLanguage) {
        // Vanilla sends e.g. "en_us"
        return clientLanguage == null ? Locale.US : Locale.forLanguageTag(clientLanguage.replace('_', '-'));
    }

    @Override
    public @NotNull UUID getUniqueId() {
        return this.player.getUUID();
    }

    @Override
    public @NotNull Locale getClientLocale() {
        return parseLocale(this.player.clientInformation().language());
    }

    @Override
    public boolean isFirstJoin() {
        return this.player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) == 0;
    }

    @Override
    public @NotNull TriState isNativeAdmin() {
        MinecraftServer server = this.player.level().getServer();
        return TriState.byBoolean(server.getPlayerList().isOp(this.player.nameAndId()));
    }

    @Override
    public void kick(@Nullable Component reason) {
        MinecraftServer server = this.player.level().getServer();
        this.player.connection.disconnect(MinecraftServerAudiences.of(server).asNative(reason != null ? reason : Component.empty()));
    }

    @Override
    public @NotNull Audience asAudience() {
        return (Audience) this.player; // ServerPlayer is made a ForwardingAudience by adventure-platform-fabric
    }

    @Override
    public @NotNull PermissionChecker asPermissionChecker() {
        return FabricPermissionUtil.forEntity(this.player);
    }

    @Override
    public @NotNull Object unwrapNative() {
        return this.player;
    }
}
