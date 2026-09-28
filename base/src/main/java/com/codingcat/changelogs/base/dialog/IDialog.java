package com.codingcat.changelogs.base.dialog;

import com.codingcat.changelogs.base.ServerChangelogs;
import com.codingcat.changelogs.base.dialog.ui.ChangelogDialog;
import com.codingcat.changelogs.base.dialog.ui.editor.ChangelogEditorDialog;
import com.codingcat.changelogs.platformapi.player.IPlayer;
import com.github.retrooper.packetevents.protocol.dialog.Dialog;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public interface IDialog {
    @NotNull Dialog build(@NotNull IPlayer player, @NotNull DialogSessionManager sessionManager);

    default void showTo(@NotNull IPlayer player, @NotNull DialogSessionManager sessionManager, @NotNull Object session, @NotNull DialogPackets.PacketPhase packetPhase) {
        sessionManager.startSession(this, player, session);
        this.showTo(player, sessionManager, packetPhase);
    }

    default void showTo(@NotNull IPlayer player, @NotNull DialogSessionManager sessionManager, @NotNull DialogPackets.PacketPhase packetPhase) {
        Dialog dialog = this.build(player, sessionManager);
        DialogPackets.showDialog(player, dialog, packetPhase);
    }

    void onActionTriggered(@NotNull String action, @Nullable NBTCompound data, @NotNull IPlayer source, @NotNull DialogSessionManager sessionManager);

    default void attemptDestroy() throws DestroyRejectedException {
    }

    @NotNull String getId();

    @RequiredArgsConstructor
    final class Holder {
        private final @NotNull ServerChangelogs plugin;
        private final Map<String, IDialog> dialogMap = new HashMap<>();

        public void recreate() {
            Consumer<IDialog> register = d -> this.dialogMap.put(d.getId(), d);
            this.dialogMap.clear();
            register.accept(new ChangelogEditorDialog(
                    plugin.getChangelogStorage(),
                    plugin.getApi(),
                    plugin.pluginConfig().useNativeFallbackPermissions()
            ));
            register.accept(new ChangelogDialog(
                    plugin.getChangelogStorage(), plugin.getApi(), this,
                    plugin.pluginConfig().getDateFormatter(),
                    plugin.pluginConfig().showChangelogHeader(),
                    plugin.pluginConfig().createChangelogHeaderStack(),
                    plugin.pluginConfig().useNativeFallbackPermissions()
            ));
        }

        public void ensureCanReload() throws DestroyRejectedException {
            for (IDialog dialog : this.dialogMap.values()) dialog.attemptDestroy();
        }

        public @NotNull IDialog getFromId(@NotNull String id) throws NullPointerException {
            return Objects.requireNonNull(this.dialogMap.get(id), "No dialog found matching ID \"" + id + "\"");
        }

        public <T extends IDialog> @NotNull T getFromType(@NotNull Class<T> cls) throws IllegalArgumentException {
            return this.dialogMap.values().stream()
                    .filter(d -> cls.isAssignableFrom(d.getClass()))
                    .findAny().map(cls::cast)
                    .orElseThrow(() -> new IllegalArgumentException("No matching dialog found"));
        }
    }

    @Getter
    @RequiredArgsConstructor
    class DestroyRejectedException extends Exception {
        private final @NotNull String key;
    }
}
