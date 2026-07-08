package com.codingcat.changelogs.command;

import com.codingcat.changelogs.ServerChangelogs;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import static io.papermc.paper.command.brigadier.Commands.literal;

public class ReloadSubCommand implements BrigadierSubCommand {
    @Override
    public @NotNull LiteralCommandNode<CommandSourceStack> build(@NotNull ServerChangelogs plugin) {
        return literal("reload").build();
    }
}
