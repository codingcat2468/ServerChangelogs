package com.codingcat.changelogs.base.dialog.ui;

import com.codingcat.changelogs.api.ServerChangelogsApi;
import com.codingcat.changelogs.base.compat.PacketEventsFix;
import com.codingcat.changelogs.base.data.ChangelogEntry;
import com.codingcat.changelogs.base.data.ChangelogStorage;
import com.codingcat.changelogs.base.dialog.DialogPackets;
import com.codingcat.changelogs.base.dialog.DialogSessionManager;
import com.codingcat.changelogs.base.dialog.IDialog;
import com.codingcat.changelogs.base.dialog.ui.editor.ChangelogEditorDialog;
import com.codingcat.changelogs.base.dialog.ui.editor.EditorSession;
import com.codingcat.changelogs.platformapi.player.IPlayer;
import com.github.retrooper.packetevents.protocol.dialog.CommonDialogData;
import com.github.retrooper.packetevents.protocol.dialog.Dialog;
import com.github.retrooper.packetevents.protocol.dialog.DialogAction;
import com.github.retrooper.packetevents.protocol.dialog.NoticeDialog;
import com.github.retrooper.packetevents.protocol.dialog.body.DialogBody;
import com.github.retrooper.packetevents.protocol.dialog.body.ItemDialogBody;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessage;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessageDialogBody;
import com.github.retrooper.packetevents.protocol.dialog.button.ActionButton;
import com.github.retrooper.packetevents.protocol.dialog.button.CommonButtonData;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import static com.codingcat.changelogs.base.dialog.ui.editor.ChangelogEditorDialog.permissionCheck;
import static com.codingcat.changelogs.base.lang.TranslationSource.translatable;
import static com.codingcat.changelogs.base.lang.TranslationSource.translatableManual;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
public class ChangelogDialog implements IDialog {
    public static final int LINE_WIDTH = 440;
    private final @Getter String id = "changelog_view";
    private final @NotNull ChangelogStorage storage;
    private final @NotNull ServerChangelogsApi api;
    private final @NotNull IDialog.Holder holder;
    private final @NotNull DateTimeFormatter dateFormatter;
    private final boolean addHeader;
    private final @Nullable ItemStack headerItem;
    private final boolean useFallbackPermissions;

    @Override
    public @NotNull Dialog build(@NotNull IPlayer p, @NotNull DialogSessionManager sessionManager) {
        boolean canManage = this.canManage(p, sessionManager);
        if (canManage) sessionManager.startSessionIfNoneActive(this, p, Object::new);
        List<DialogBody> body = this.storage.listEntries()
                .reversed().stream()
                .map(e -> formatEntry(e, p, sessionManager, canManage))
                .map(c -> (DialogBody) new PlainMessageDialogBody(new PlainMessage(c, LINE_WIDTH)))
                .toList();
        if (body.isEmpty())
            body = List.of(new PlainMessageDialogBody(new PlainMessage(translatableManual(p, "dialog.changelog.empty"), 350)));
        if (this.addHeader) {
            body = new ArrayList<>(body);
            PlainMessage headerMessage = new PlainMessage(translatableManual(p, "dialog.changelog.header"), 260);
            ItemDialogBody itemBody = null;
            if (headerItem != null) {
                itemBody = new ItemDialogBody(headerItem, headerMessage, false, false, 17, 17);
                PacketEventsFix.fixItemBody(itemBody);
            }
            body.addFirst(headerItem != null ? itemBody : new PlainMessageDialogBody(headerMessage));
        }
        CommonDialogData common = new CommonDialogData(
                translatableManual(p, "dialog.changelog.title"),
                null, true, false,
                canManage ? DialogAction.NONE : DialogAction.CLOSE, body, List.of()
        );
        ActionButton button = new ActionButton(new CommonButtonData(
                translatableManual(p, "dialog.changelog.button.close"),
                null, 60
        ), sessionManager.createStaticAction(this, "confirm_read"));
        return new NoticeDialog(common, button);
    }

    private @NotNull Component formatEntry(@NotNull ChangelogEntry entry, @NotNull IPlayer player, @NotNull DialogSessionManager sessionManager, boolean canManage) {
        Component linesComponent = createLinesComponent(player, null, entry.lines());
        Component entryComponent = translatableManual(player, "dialog.changelog.entry" + (this.storage.isUnreadFor(entry, player.getUniqueId()) ? "_unread" : ""),
                text(dateFormatter.format(entry.recordedAt())), linesComponent,
                Objects.requireNonNullElseGet(entry.author(), () -> translatableManual(player, "dialog.changelog.unspecified_author"))
        );
        if (canManage) {
            String prefix = "dialog.changelog.manage.changelog_action.";
            NBTCompound payload = new NBTCompound();
            payload.setTag("uid", new NBTInt(entry.uid()));
            Component edit = translatableManual(player, prefix + "format", translatable(prefix + "edit"))
                    .clickEvent(sessionManager.createSessionBasedClickEvent(this, "start_editing", payload));
            Component delete = translatableManual(player, prefix + "format", translatable(prefix + "delete"))
                    .clickEvent(sessionManager.createSessionBasedClickEvent(this, "request_delete", payload));
            entryComponent = entryComponent.appendSpace().append(edit).appendSpace().append(delete);
        }
        return entryComponent;
    }

    public static @NotNull Component createLinesComponent(@NotNull IPlayer player, @Nullable BiFunction<Component, Integer, Component> mapper, @NotNull List<Component> lines) {
        if (mapper == null) mapper = (c, _) -> c;
        Component component = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            line = translatableManual(player, "dialog.changelog.entry_line", line);
            line = mapper.apply(line, i);
            component = component.append(line);
            if (i < lines.size() - 1) component = component.appendNewline();
        }
        return component;
    }

    @Override
    public void onActionTriggered(@NotNull String action, @Nullable NBTCompound data, @NotNull IPlayer source, @NotNull DialogSessionManager sessionManager) {
        try {
            boolean canManage = this.canManage(source, sessionManager);
            switch (action) {
                case "confirm_read" -> {
                    List<Integer> uids = this.storage.listEntries()
                            .stream()
                            .filter(e -> this.storage.isUnreadFor(e, source.getUniqueId()))
                            .map(ChangelogEntry::uid)
                            .toList();
                    uids.forEach(uid -> api.markAsRead(uid, source.getUniqueId()));
                    if (canManage) {
                        sessionManager.endSession(source);
                        DialogPackets.clearDialog(source, DialogPackets.PacketPhase.PLAY);
                    }
                    if (!uids.isEmpty())
                        source.asAudience().sendMessage(translatable("dialog.changelog.read", text(uids.size())));
                }
                case "reopen" -> {
                    if (canManage) this.showTo(source, sessionManager, DialogPackets.PacketPhase.PLAY);
                }
                case "start_editing" -> {
                    if (data == null || !canManage) return;
                    int uid = data.getNumberTagValueOrThrow("uid").intValue();
                    ChangelogEntry entry = this.storage.getByUID(uid);
                    if (entry == null) return;
                    sessionManager.endSession(source);
                    EditorSession.Edit session = new EditorSession.Edit(entry);
                    this.holder.getFromType(ChangelogEditorDialog.class)
                            .showTo(source, sessionManager, session, DialogPackets.PacketPhase.PLAY);
                }
                case "request_delete" -> {
                    if (data == null || !canManage) return;
                    int uid = data.getNumberTagValueOrThrow("uid").intValue();
                    NBTCompound payload = new NBTCompound();
                    payload.setTag("uid", new NBTInt(uid));
                    String prefix = "dialog.changelog.manage.confirm_delete.";
                    DialogPackets.showSimpleConfirm(source, prefix + "title", prefix + "content",
                            sessionManager.createSessionBasedAction(this, "confirm_delete", payload, false), true,
                            sessionManager.createSessionBasedAction(this, "reopen", false), DialogPackets.PacketPhase.PLAY);
                }
                case "confirm_delete" -> {
                    if (data == null || !canManage) return;
                    int uid = data.getNumberTagValueOrThrow("uid").intValue();
                    this.storage.removeEntry(uid);
                    this.showTo(source, sessionManager, DialogPackets.PacketPhase.PLAY);
                }
            }
        } finally {
            // Ensure the player is able to continue gameplay even if any exceptions occur
            if (action.equals("confirm_read")) sessionManager.unfreeze(source);
        }
    }

    private boolean canManage(@NotNull IPlayer player, @NotNull DialogSessionManager sessionManager) {
        return permissionCheck("manage", player, useFallbackPermissions) && !sessionManager.isFrozen(player);
    }
}
