package com.codingcat.changelogs.fabric.command;

import com.codingcat.changelogs.fabric.player.FabricPermissionUtil;
import com.codingcat.changelogs.fabric.player.FabricPlayerManager;
import com.codingcat.changelogs.platformapi.command.ICommandSource;
import com.codingcat.changelogs.platformapi.player.IPlayer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class WrappedCommandSourceStack implements ICommandSource {
    private final @NotNull CommandSourceStack sourceStack;
    private final @NotNull FabricPlayerManager playerManager;

    @Override
    public @Nullable IPlayer getExecutingPlayer() {
        ServerPlayer player = this.sourceStack.getPlayer();
        return player != null ? this.playerManager.fromNative(player) : null;
    }

    @Override
    public @NotNull Audience asAudience() {
        return MinecraftServerAudiences.of(this.sourceStack.getServer()).audience(this.sourceStack);
    }

    @Override
    public @NotNull PermissionChecker asPermissionChecker() {
        return FabricPermissionUtil.forSource(this.sourceStack);
    }

    @Override
    public @NotNull Object unwrapNative() {
        return this.sourceStack;
    }
}
