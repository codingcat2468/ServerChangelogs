package com.codingcat.changelogs.base.dialog;

import com.codingcat.changelogs.base.ServerChangelogs;
import com.codingcat.changelogs.platformapi.player.IPlayer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.event.ClickEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.action.StaticAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class DialogSessionManager {
    private static final @NotNull Function<String, Key> KEY_GENERATOR = path -> ServerChangelogs.KEY_GENERATOR.apply("dialog/" + path);
    private final Set<Key> staticActions = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Object> sessionData = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<?>> frozenViewers = new ConcurrentHashMap<>();
    private final @NotNull IDialog.Holder dialogHolder;

    /** Thrown by the payload helpers below for missing/mistyped fields, i.e. modified clients. */
    public static final class InvalidPayloadException extends RuntimeException {
        InvalidPayloadException(@NotNull String key) {
            super("Missing or invalid payload field \"" + key + "\"");
        }
    }

    public static int requireInt(@NotNull CompoundTag tag, @NotNull String key) {
        return tag.getInt(key).orElseThrow(() -> new InvalidPayloadException(key));
    }

    public static @NotNull String requireString(@NotNull CompoundTag tag, @NotNull String key) {
        return tag.getString(key).orElseThrow(() -> new InvalidPayloadException(key));
    }

    public static boolean requireBoolean(@NotNull CompoundTag tag, @NotNull String key) {
        return tag.getBoolean(key).orElseThrow(() -> new InvalidPayloadException(key));
    }

    /**
     * Handles a custom click action packet. Click events inside of text can't carry a payload that survives
     * the Adventure -> vanilla conversion, so they encode one integer in the ID: {@code <action>.<field>.<value>}.
     */
    public void handleCustomClick(@NotNull Identifier id, @NotNull Optional<Tag> payload, @NotNull IPlayer player) {
        if (!id.getNamespace().equals(ServerChangelogs.NAMESPACE)) return;
        String path = id.getPath();
        int fSIdx, lSIdx;
        if ((fSIdx = path.indexOf('/')) == -1 || !path.startsWith("dialog/") || (fSIdx + 1) >= (lSIdx = path.lastIndexOf('/'))
                || lSIdx == (path.length() - 1)) return;
        String dialogId = path.substring(fSIdx + 1, lSIdx);
        String[] actionParts = path.substring(lSIdx + 1).split("\\.");
        String actionId = actionParts[0];
        Key key = KEY_GENERATOR.apply(dialogId + "/" + actionId);
        if (!dialogId.equals(activeSessions.get(player.getUniqueId())) && !staticActions.contains(key)) return;
        IDialog dialog;
        try {
            dialog = this.dialogHolder.getFromId(dialogId);
        } catch (NullPointerException e) {
            return;
        }
        CompoundTag data = payload.filter(CompoundTag.class::isInstance).map(CompoundTag.class::cast).orElse(null);
        if (actionParts.length == 3) {
            try {
                data = new CompoundTag();
                data.putInt(actionParts[1], Integer.parseInt(actionParts[2]));
            } catch (NumberFormatException _) {
                return;
            }
        }
        try {
            dialog.onActionTriggered(actionId, data, player, this);
        } catch (InvalidPayloadException _) {
            // Ignore invalid or unexpected payloads like malicious packets sent by a modified client
        } catch (Exception e) {
            throw new RuntimeException("Failed to run dialog action handler for \"" + dialogId + "\", action \"" + actionId + "\"", e);
        }
    }

    public @NotNull Action createStaticAction(@NotNull IDialog dialog, @NotNull String id) {
        Key key = this.createStaticKey(dialog, id);
        return new StaticAction(new net.minecraft.network.chat.ClickEvent.Custom(toIdentifier(key), Optional.empty()));
    }

    public @NotNull Action createSessionBasedAction(@NotNull IDialog dialog, @NotNull String id, boolean includeInputs) {
        return this.createSessionBasedAction(dialog, id, null, includeInputs);
    }

    public @NotNull Action createSessionBasedAction(@NotNull IDialog dialog, @NotNull String id, @Nullable CompoundTag data, boolean includeInputs) {
        Identifier identifier = toIdentifier(this.createSessionBasedKey(dialog, id));
        return includeInputs ? new CustomAll(identifier, Optional.ofNullable(data))
                : new StaticAction(new net.minecraft.network.chat.ClickEvent.Custom(identifier, Optional.ofNullable(data)));
    }

    public @NotNull ClickEvent<?> createSessionBasedClickEvent(@NotNull IDialog dialog, @NotNull String id) {
        return ClickEvent.custom(this.createSessionBasedKey(dialog, id), null);
    }

    public @NotNull ClickEvent<?> createSessionBasedClickEvent(@NotNull IDialog dialog, @NotNull String id, @NotNull String field, int value) {
        return ClickEvent.custom(this.createSessionBasedKey(dialog, id + "." + field + "." + value), null);
    }

    private static @NotNull Identifier toIdentifier(@NotNull Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }

    private @NotNull Key createSessionBasedKey(@NotNull IDialog dialog, @NotNull String id) {
        return KEY_GENERATOR.apply(dialog.getId() + "/" + id);
    }

    private @NotNull Key createStaticKey(@NotNull IDialog dialog, @NotNull String id) {
        Key key = this.createSessionBasedKey(dialog, id);
        this.staticActions.add(key);
        return key;
    }

    public void startSessionIfNoneActive(@NotNull IDialog dialog, @NotNull IPlayer player, @NotNull Supplier<Object> initialDataSupplier) {
        if (this.isSessionActive(player, dialog)) return;
        this.startSession(dialog, player, initialDataSupplier.get());
    }

    public void startSession(@NotNull IDialog dialog, @NotNull IPlayer player, @NotNull Object initialData) {
        this.setSessionData(player, initialData);
        this.activeSessions.put(player.getUniqueId(), dialog.getId());
    }

    public void endSession(@NotNull IPlayer player) {
        this.endSession(player.getUniqueId());
    }

    public void endSession(@NotNull UUID playerId) {
        this.activeSessions.remove(playerId);
        this.sessionData.remove(playerId);
    }

    public boolean isSessionActive(@NotNull IPlayer player, @NotNull IDialog dialog) {
        return dialog.getId().equals(this.activeSessions.get(player.getUniqueId()));
    }

    public void setSessionData(@NotNull IPlayer player, @NotNull Object data) {
        this.sessionData.put(player.getUniqueId(), data);
    }

    public <T> @NotNull T getSessionData(@NotNull IPlayer player, @NotNull Class<T> dataType) throws ClassCastException {
        return dataType.cast(this.sessionData.get(player.getUniqueId()));
    }

    public boolean isFrozen(@NotNull IPlayer player) {
        CompletableFuture<?> future = this.frozenViewers.get(player.getUniqueId());
        return future != null && !future.isDone();
    }

    public void unfreeze(@NotNull IPlayer player) {
        this.unfreeze(player.getUniqueId());
    }

    public void unfreeze(@NotNull UUID playerId) {
        CompletableFuture<?> future = this.frozenViewers.get(playerId);
        if (future != null) future.complete(null);
    }

    public void freeze(@NotNull IPlayer player) {
        this.frozenViewers.computeIfAbsent(player.getUniqueId(), _ -> new CompletableFuture<>());
    }

    public void waitForUnfreeze(@NotNull IPlayer player) {
        CompletableFuture<?> future = this.frozenViewers.get(player.getUniqueId());
        if (future == null) return;
        try {
            future.join();
        } catch (CancellationException | CompletionException _) {
        }
        if (future.isDone()) this.frozenViewers.remove(player.getUniqueId());
    }
}
