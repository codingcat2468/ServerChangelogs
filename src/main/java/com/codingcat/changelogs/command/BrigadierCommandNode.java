package com.codingcat.changelogs.command;

import com.codingcat.changelogs.ServerChangelogs;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface BrigadierCommandNode {
    @NotNull Set<BrigadierCommandNode> SUB_COMMANDS = DialogSubCommands.withDialogs(new ReloadSubCommand());

    @NotNull LiteralCommandNode<CommandSourceStack> build(@NotNull ServerChangelogs plugin);
}
