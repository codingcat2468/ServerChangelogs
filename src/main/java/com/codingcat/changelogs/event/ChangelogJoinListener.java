package com.codingcat.changelogs.event;

import com.codingcat.changelogs.data.ChangelogStorage;
import com.codingcat.changelogs.dialog.ChangelogDialog;
import com.codingcat.changelogs.dialog.IDialog;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class ChangelogJoinListener implements Listener {
    private final @NotNull Supplier<ChangelogStorage> storageSupplier;
    private final @NotNull IDialog.Holder dialogHolder;

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        if (!event.getPlayer().hasPlayedBefore()) return;
        boolean unreadChangelogs = this.storageSupplier.get().listEntries().stream()
                .anyMatch(e -> !e.hasRead(event.getPlayer()));
        if (!unreadChangelogs) return;
        ChangelogDialog dialog = this.dialogHolder.getFromType(ChangelogDialog.class);
        dialog.showTo(event.getPlayer());
    }
}
