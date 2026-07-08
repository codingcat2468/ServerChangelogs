package com.codingcat.changelogs.command;

import com.codingcat.changelogs.ServerChangelogs;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import static com.codingcat.changelogs.lang.TranslationSource.translatable;
import static io.papermc.paper.command.brigadier.Commands.literal;
import static net.kyori.adventure.text.Component.text;

public class ReloadSubCommand implements BrigadierSubCommand {
    @Override
    public @NotNull LiteralCommandNode<CommandSourceStack> build(@NotNull ServerChangelogs plugin) {
        return literal("reload").executes(ctx -> {
            long time = System.currentTimeMillis();
            plugin.reload();
            long took = System.currentTimeMillis() - time;
            ctx.getSource().getSender().sendMessage(translatable("command.reload.success", text(took)));
            return Command.SINGLE_SUCCESS;
        }).build();
    }
}
