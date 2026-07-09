package com.codingcat.changelogs.command;

import com.codingcat.changelogs.ServerChangelogs;
import com.codingcat.changelogs.dialog.ChangelogDialog;
import com.codingcat.changelogs.dialog.CreateChangelogDialog;
import com.codingcat.changelogs.dialog.IDialog;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.codingcat.changelogs.lang.TranslationSource.translatable;
import static io.papermc.paper.command.brigadier.Commands.literal;

public class DialogSubCommands {
    public static @NotNull Set<BrigadierCommandNode> withDialogs(@NotNull BrigadierCommandNode... commands) {
        Set<BrigadierCommandNode> commandSet = new HashSet<>();
        commandSet.add(new Command("create", CreateChangelogDialog.class));
        commandSet.add(new Command("view", ChangelogDialog.class));
        commandSet.addAll(Arrays.stream(commands).toList());
        return commandSet;
    }

    public static @NotNull LiteralCommandNode<CommandSourceStack> buildDedicatedChangelogCommand(@NotNull ServerChangelogs plugin) {
        return new Command("changelog", ChangelogDialog.class).build(plugin);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class Command implements BrigadierCommandNode {
        private final @NotNull String name;
        private final @NotNull Class<? extends IDialog> dialogCls;

        @Override
        public @NotNull LiteralCommandNode<CommandSourceStack> build(@NotNull ServerChangelogs plugin) {
            return literal(this.name).executes(ctx -> {
                if (!(ctx.getSource().getSender() instanceof Player player)) {
                    ctx.getSource().getSender().sendMessage(translatable("command.onlyplayer"));
                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                }
                plugin.getDialogHolder().getFromType(this.dialogCls).showTo(player);
                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
            }).build();
        }
    }
}
