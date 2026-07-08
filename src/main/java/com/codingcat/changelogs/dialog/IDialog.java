package com.codingcat.changelogs.dialog;

import io.papermc.paper.dialog.Dialog;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface IDialog {
    @NotNull Dialog build(@NotNull Player player);

    default void showTo(@NotNull Player player) {
        Dialog dialog = this.build(player);
        player.showDialog(dialog);
    }
}
