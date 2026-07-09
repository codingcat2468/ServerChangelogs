package com.codingcat.changelogs.dialog;

import com.codingcat.changelogs.ServerChangelogs;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static com.codingcat.changelogs.ServerChangelogs.error;

public interface IDialog {
    @NotNull Dialog build(@NotNull Player player);

    default void showTo(@NotNull Player player) {
        Dialog dialog = this.build(player);
        player.showDialog(dialog);
    }

    @SuppressWarnings("UnstableApiUsage")
    void onActionTriggered(@NotNull String action, @NotNull DialogResponseView data, @NotNull Player source);

    @SuppressWarnings("UnstableApiUsage")
    default @NotNull DialogAction action(@NotNull String id) {
        return DialogAction.customClick((response, audience) -> {
            if (!(audience instanceof Player player)) return;
            try {
                this.onActionTriggered(id, response, player);
            } catch (Throwable e) {
                error("dialog.error", e);
            }
        }, ClickCallback.Options.builder().uses(1).build());
    }

    @RequiredArgsConstructor
    final class Holder {
        private final @NotNull ServerChangelogs plugin;
        private final Set<IDialog> dialogSet = new HashSet<>();

        public void recreate() {
            this.dialogSet.clear();
            this.dialogSet.add(new CreateChangelogDialog(plugin.getChangelogStorage()));
            this.dialogSet.add(new ChangelogDialog(
                    plugin.getChangelogStorage(),
                    plugin.pluginConfig().getDateFormatter(),
                    plugin.pluginConfig().showChangelogHeader(),
                    plugin.pluginConfig().createChangelogHeaderStack()
            ));
        }

        public <T extends IDialog> @NotNull T getFromType(@NotNull Class<T> cls) throws IllegalArgumentException {
            return this.dialogSet.stream()
                    .filter(d -> cls.isAssignableFrom(d.getClass()))
                    .findAny().map(cls::cast)
                    .orElseThrow(() -> new IllegalArgumentException("No matching dialog found"));
        }
    }
}
