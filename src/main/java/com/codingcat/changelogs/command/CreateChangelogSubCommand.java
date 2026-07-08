package com.codingcat.changelogs.command;

import com.codingcat.changelogs.ServerChangelogs;
import com.codingcat.changelogs.dialog.CreateChangelogDialog;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static com.codingcat.changelogs.lang.TranslationSource.translatable;
import static io.papermc.paper.command.brigadier.Commands.literal;

public class CreateChangelogSubCommand implements BrigadierSubCommand {
    @Override
    public @NotNull LiteralCommandNode<CommandSourceStack> build(@NotNull ServerChangelogs plugin) {
        return literal("create").executes(ctx -> {
            if (!(ctx.getSource().getSender() instanceof Player player)) {
                ctx.getSource().getSender().sendMessage(translatable("command.onlyplayer"));
                return Command.SINGLE_SUCCESS;
            }
            plugin.getDialogHolder().getFromType(CreateChangelogDialog.class).showTo(player);
            return Command.SINGLE_SUCCESS;
        }).build();
    }
}
