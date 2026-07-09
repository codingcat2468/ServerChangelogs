package com.codingcat.changelogs.command;

import com.codingcat.changelogs.ServerChangelogs;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.codingcat.changelogs.lang.TranslationSource.translatable;
import static com.codingcat.changelogs.ServerChangelogs.error;
import static io.papermc.paper.command.brigadier.Commands.literal;
import static net.kyori.adventure.text.Component.text;

public class ReloadSubCommand implements BrigadierCommandNode {
    @Override
    public @NotNull LiteralCommandNode<CommandSourceStack> build(@NotNull ServerChangelogs plugin) {
        return literal("reload")
                .requires(BrigadierCommandNode.requirePermission("command.reload"))
                .executes(ctx -> {
                    long time = System.currentTimeMillis();
                    Component errorMsg = null;
                    try {
                        plugin.reload();
                        long took = System.currentTimeMillis() - time;
                        ctx.getSource().getSender().sendMessage(translatable("command.reload.success", text(took)));
                    } catch (IOException e) {
                        errorMsg = translatable("command.reload.error.io");
                        error("command.reload.error.details", e);
                    } catch (InvalidConfigurationException e) {
                        errorMsg = translatable("command.reload.error.invalid", text(e.getMessage()));
                    }
                    if (errorMsg != null) ctx.getSource().getSender().sendMessage(errorMsg);
                    return Command.SINGLE_SUCCESS;
                }).build();
    }
}
