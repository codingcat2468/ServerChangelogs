package com.codingcat.changelogs.paper.command;

import com.codingcat.changelogs.paper.player.PaperPlayerManager;
import com.codingcat.changelogs.platformapi.command.ICommandSource;
import com.codingcat.changelogs.platformapi.player.IPlayer;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.permission.PermissionChecker;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class WrappedCommandSourceStack implements ICommandSource {
    private final @NotNull CommandSourceStack sourceStack;
    private final @NotNull PaperPlayerManager playerManager;

    @Override
    public @Nullable IPlayer getExecutingPlayer() {
        return (this.sourceStack.getSender() instanceof Player p) ? playerManager.fromNative(p) : null;
    }

    @Override
    public @NotNull Audience asAudience() {
        return this.sourceStack.getSender();
    }

    @Override
    public @NotNull PermissionChecker asPermissionChecker() {
        return this.sourceStack.getSender()::permissionValue;
    }

    @Override
    public @NotNull Object unwrapNative() {
        return this.sourceStack;
    }
}
