package com.codingcat.changelogs.command;

import com.codingcat.changelogs.ServerChangelogs;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Predicate;

public interface BrigadierCommandNode {
    @NotNull Set<BrigadierCommandNode> SUB_COMMANDS = DialogSubCommands.withDialogs(new ReloadSubCommand());

    @NotNull LiteralCommandNode<CommandSourceStack> build(@NotNull ServerChangelogs plugin);

    static @NotNull Predicate<CommandSourceStack> requirePermission(@NotNull String permission) {
        String perm = ServerChangelogs.NAMESPACE + "." + permission;
        return ctx -> ctx.getSender().hasPermission(perm)
                && (ctx.getExecutor() == null || ctx.getExecutor().hasPermission(perm));
    }
}
