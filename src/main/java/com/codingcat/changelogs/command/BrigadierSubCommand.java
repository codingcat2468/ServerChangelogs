package com.codingcat.changelogs.command;

import com.codingcat.changelogs.ServerChangelogs;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface BrigadierSubCommand {
    @NotNull Set<BrigadierSubCommand> COMMANDS = Set.of(new ReloadSubCommand());

    @NotNull LiteralCommandNode<CommandSourceStack> build(@NotNull ServerChangelogs plugin);
}
