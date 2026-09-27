package com.codingcat.changelogs.base.dialog.ui.editor;

import com.codingcat.changelogs.base.ServerChangelogs;
import com.codingcat.changelogs.base.compat.PacketEventsFix;
import com.codingcat.changelogs.base.data.ChangelogStorage;
import com.codingcat.changelogs.base.dialog.DialogPackets;
import com.codingcat.changelogs.base.dialog.DialogSessionManager;
import com.codingcat.changelogs.base.dialog.IDialog;
import com.codingcat.changelogs.base.dialog.ui.ChangelogDialog;
import com.codingcat.changelogs.platformapi.player.IPlayer;
import com.github.retrooper.packetevents.protocol.dialog.CommonDialogData;
import com.github.retrooper.packetevents.protocol.dialog.Dialog;
import com.github.retrooper.packetevents.protocol.dialog.DialogAction;
import com.github.retrooper.packetevents.protocol.dialog.MultiActionDialog;
import com.github.retrooper.packetevents.protocol.dialog.action.Action;
import com.github.retrooper.packetevents.protocol.dialog.body.DialogBody;
import com.github.retrooper.packetevents.protocol.dialog.body.ItemDialogBody;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessage;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessageDialogBody;
import com.github.retrooper.packetevents.protocol.dialog.button.ActionButton;
import com.github.retrooper.packetevents.protocol.dialog.button.CommonButtonData;
import com.github.retrooper.packetevents.protocol.dialog.input.Input;
import com.github.retrooper.packetevents.protocol.dialog.input.TextInputControl;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.codingcat.changelogs.base.dialog.ui.ChangelogDialog.createLinesComponent;
import static com.codingcat.changelogs.base.lang.TranslationSource.translatable;
import static com.codingcat.changelogs.base.lang.TranslationSource.translatableManual;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
public class ChangelogEditorDialog implements IDialog {
    private static final @NotNull Function<EditorSession, String> TITLE_KEY = s -> "dialog.editor." + s.getId() + ".title";
    private final @NotNull Map<UUID, EditorSession> savedSessions = new ConcurrentHashMap<>();
    private final @Getter String id = "changelog_editor";
    private final @NotNull ChangelogStorage storage;
    private final boolean useFallbackPermissions;
    private final @NotNull List<String> predefinedAuthors;

    @Override
    public @NotNull Dialog build(@NotNull IPlayer p, @NotNull DialogSessionManager sessionManager) {
        sessionManager.startSessionIfNoneActive(this, p, () -> {
            EditorSession session = this.savedSessions.remove(p.getUniqueId());
            if (session == null || !session.canBeSaved()) session = new EditorSession.Create(storage.nextUID());
            return session;
        });
        EditorSession session = sessionManager.getSessionData(p, EditorSession.class);
        String sessionTranslation = "dialog.editor." + session.getId();
        List<Component> previewLines = session.deserializeLines();
        BiFunction<Component, Integer, Component> lineMapper = (line, idx) -> {
            boolean isEditing = session.getEditingLineIndex() == idx;
            if (isEditing) line = line.decoration(TextDecoration.BOLD, true);
            String prefix = "dialog.editor.line_action.";
            NBTCompound payload = new NBTCompound();
            payload.setTag("target_line", new NBTInt(idx));
            Component edit = null;
            if (!isEditing) edit = translatableManual(p, prefix + "format", translatable(prefix + "edit"))
                    .decoration(TextDecoration.BOLD, false)
                    .clickEvent(sessionManager.createSessionBasedClickEvent(this, "start_edit_line", payload));
            Component remove = translatableManual(p, prefix + "format", translatable(prefix + "remove"))
                    .decoration(TextDecoration.BOLD, false)
                    .clickEvent(sessionManager.createSessionBasedClickEvent(this, "remove_line", payload));
            Component finalCmp = line.appendSpace();
            if (!isEditing) finalCmp = finalCmp.append(edit).appendSpace();
            return finalCmp.append(remove);
        };
        ItemDialogBody itemBody = new ItemDialogBody(ItemStack.builder().type(ItemTypes.WRITABLE_BOOK).build(),
                new PlainMessage(translatableManual(p, sessionTranslation + ".subtitle", text(session.getEntryUID() + 1)), 160),
                false, false, 15, 15);
        PacketEventsFix.fixItemBody(itemBody);
        List<DialogBody> body = new ArrayList<>(List.of(itemBody,
                new PlainMessageDialogBody(new PlainMessage(translatableManual(p, "dialog.editor.hint"), ChangelogDialog.LINE_WIDTH)),
                new PlainMessageDialogBody(new PlainMessage(!previewLines.isEmpty() ? createLinesComponent(p, lineMapper, previewLines) : translatableManual(p, "dialog.editor.empty_preview"), ChangelogDialog.LINE_WIDTH))
        ));
        if (session.isShowRestoredMessage())
            body.add(1, new PlainMessageDialogBody(new PlainMessage(translatableManual(p, "dialog.editor.restored_session"), 400)));
        if (!predefinedAuthors.isEmpty())
            body.add(new PlainMessageDialogBody(new PlainMessage(createPredefinedAuthorsComponent(p, sessionManager), ChangelogDialog.LINE_WIDTH)));
        List<Input> inputs = List.of(
                new Input("line", new TextInputControl(350, translatableManual(p, "dialog.editor.input.contents"),
                        true, session.getCurrentLine(), 5000, null)),
                new Input("author", new TextInputControl(200, translatableManual(p, "dialog.editor.input.author"), true, session.getAuthor(), 200, null))
        );
        CommonDialogData common = new CommonDialogData(
                translatableManual(p, TITLE_KEY.apply(session)),
                null, true, false,
                DialogAction.NONE, body, inputs
        );
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(new ActionButton(new CommonButtonData(translatableManual(p, sessionTranslation + ".commit_button"), null, 160), sessionManager.createSessionBasedAction(this, "commit", true)));
        String lineAction = session.getEditingLineIndex() != -1 ? "edit_line" : "add_line";
        buttons.add(new ActionButton(new CommonButtonData(translatableManual(p, "dialog.editor.button." + lineAction), null, 100), sessionManager.createSessionBasedAction(this, lineAction, true)));
        ActionButton closeBtn = new ActionButton(new CommonButtonData(translatableManual(p, "dialog.editor.button.close"), null, 100), sessionManager.createSessionBasedAction(this, "try_close", true));
        return new MultiActionDialog(common, buttons, closeBtn, 3);
    }

    private @NotNull Component createPredefinedAuthorsComponent(@NotNull IPlayer p, @NotNull DialogSessionManager sessionManager) {
        String prefix = "dialog.editor.predefined_authors.";
        Component entries = Component.empty();
        for (int i = 0; i < predefinedAuthors.size(); i++) {
            Component parsedAuthor = MiniMessage.miniMessage().deserialize(predefinedAuthors.get(i));
            NBTCompound payload = new NBTCompound();
            payload.setTag("author_index", new NBTInt(i));
            Component entry = translatableManual(p, prefix + "entry", parsedAuthor)
                    .clickEvent(sessionManager.createSessionBasedClickEvent(this, "select_predefined_author", payload));
            entries = entries.append(entry);
            if (i < predefinedAuthors.size() - 1) entries = entries.appendSpace();
        }
        return translatableManual(p, prefix + "label", entries);
    }

    private @NotNull Dialog buildConfirmCloseDialog(@NotNull IPlayer p, @NotNull DialogSessionManager sessionManager) {
        EditorSession session = sessionManager.getSessionData(p, EditorSession.class);
        String prefix = "dialog.editor.confirm_close.";
        CommonDialogData common = DialogPackets.createSimpleDialog(p, TITLE_KEY.apply(session), prefix + "content", false, false);
        Function<Boolean, Action> closeActionFunc = save -> {
            NBTCompound payload = new NBTCompound();
            payload.setTag("save_session", new NBTByte(save));
            return sessionManager.createSessionBasedAction(this, "close", payload, false);
        };
        ActionButton saveSession = new ActionButton(new CommonButtonData(translatableManual(p, prefix + "button.save_session"), null, 140), closeActionFunc.apply(true));
        ActionButton discardSession = new ActionButton(new CommonButtonData(translatableManual(p, prefix + "button.discard_session"), null, 140), closeActionFunc.apply(false));
        ActionButton cancel = new ActionButton(new CommonButtonData(translatableManual(p, prefix + "button.cancel"), null, 100), sessionManager.createSessionBasedAction(this, "reopen", false));
        return new MultiActionDialog(common, List.of(saveSession, discardSession, cancel), null, 3);
    }

    private void actuallyClose(@NotNull IPlayer source, @NotNull DialogSessionManager sessionManager) {
        sessionManager.endSession(source);
        DialogPackets.clearDialog(source, DialogPackets.PacketPhase.PLAY);
    }

    @Override
    public void onActionTriggered(@NotNull String action, @Nullable NBTCompound data, @NotNull IPlayer source, @NotNull DialogSessionManager sessionManager) {
        EditorSession session = sessionManager.getSessionData(source, EditorSession.class);
        session.setShowRestoredMessage(false);
        if (data != null && data.contains("line")) {
            String rawLine = data.getStringTagValueOrThrow("line");
            String rawAuthor = data.getStringTagValueOrThrow("author");
            session.setCurrentLine(rawLine);
            session.setAuthor(rawAuthor);
        }
        switch (action) {
            case "try_close" -> {
                if (session.canBeSaved()) {
                    Dialog dialog = this.buildConfirmCloseDialog(source, sessionManager);
                    DialogPackets.showDialog(source, dialog, DialogPackets.PacketPhase.PLAY);
                    return;
                }
                this.actuallyClose(source, sessionManager);
            }
            case "close" -> {
                if (data == null) return;
                boolean saveSession = data.getBooleanOrThrow("save_session");
                if (saveSession) {
                    session.setShowRestoredMessage(true);
                    this.savedSessions.put(source.getUniqueId(), session);
                }
                this.actuallyClose(source, sessionManager);
            }
            case "reopen" -> this.showTo(source, sessionManager, DialogPackets.PacketPhase.PLAY);
            case "add_line", "edit_line" -> {
                if (action.equals("edit_line") && (session.getEditingLineIndex() == -1)) return;
                boolean removed = false;
                if (session.getCurrentLine().isBlank()) {
                    if (action.equals("add_line")) {
                        this.showRetry(source, "add_empty_line", session, sessionManager);
                        return;
                    } else {
                        session.getRawLines().remove(session.getEditingLineIndex());
                        removed = true;
                    }
                }
                if (action.equals("edit_line")) {
                    if (!removed) session.getRawLines().set(session.getEditingLineIndex(), session.getCurrentLine());
                    session.setEditingLineIndex(-1);
                } else session.getRawLines().add(session.getCurrentLine());
                session.setCurrentLine("");
                this.showTo(source, sessionManager, DialogPackets.PacketPhase.PLAY);
            }
            case "select_predefined_author" -> {
                if (data == null) return;
                int authorIndex = data.getNumberTagValueOrThrow("author_index").intValue();
                if (authorIndex < 0 || authorIndex >= predefinedAuthors.size()) return;
                session.setAuthor(predefinedAuthors.get(authorIndex));
                this.showTo(source, sessionManager, DialogPackets.PacketPhase.PLAY);
            }
            case "start_edit_line", "remove_line" -> {
                if (data == null) return;
                int lineIdx = data.getNumberTagValueOrThrow("target_line").intValue();
                if (lineIdx < 0 || lineIdx >= session.getRawLines().size()) return;
                if (action.equals("start_edit_line")) {
                    String line = session.getRawLines().get(lineIdx);
                    session.setCurrentLine(line);
                    session.setEditingLineIndex(lineIdx);
                } else {
                    if (session.getEditingLineIndex() == lineIdx) session.setEditingLineIndex(-1);
                    session.getRawLines().remove(lineIdx);
                    session.setCurrentLine("");
                }
                this.showTo(source, sessionManager, DialogPackets.PacketPhase.PLAY);
            }
            case "commit" -> {
                if (!permissionCheck(session.getPermission(), source, useFallbackPermissions)) {
                    sessionManager.endSession(source);
                    DialogPackets.showSimpleNotice(source, TITLE_KEY.apply(session), "dialog.editor.error.no_permission");
                    return;
                }
                if (data == null) return;
                if (session.getRawLines().isEmpty()) {
                    this.showRetry(source, "no_lines", session, sessionManager);
                    return;
                }
                try {
                    session.commit(this.storage);
                } catch (EditorSession.CommitException e) {
                    this.showRetry(source, session.getId() + "." + e.getTranslationKeyPart(), session, sessionManager);
                    return;
                }
                sessionManager.endSession(source);
                DialogPackets.showSimpleNotice(source, TITLE_KEY.apply(session), "dialog.editor." + session.getId() + ".success");
            }
        }
    }

    @Override
    public void attemptDestroy() throws DestroyRejectedException {
        if (!this.savedSessions.isEmpty()) throw new DestroyRejectedException("saved_sessions");
    }

    private void showRetry(@NotNull IPlayer source, @NotNull String errorPart, @NotNull EditorSession session, @NotNull DialogSessionManager sessionManager) {
        DialogPackets.showSimpleNotice(source, TITLE_KEY.apply(session), "dialog.editor.error." + errorPart, sessionManager.createSessionBasedAction(this, "reopen", false), DialogPackets.PacketPhase.PLAY);
    }

    public static boolean permissionCheck(@NotNull String permission, @NotNull IPlayer source, boolean useFallbackPermissions) {
        return useFallbackPermissions ? source.isNativeAdmin().toBooleanOrElse(false) : source.hasPermission(ServerChangelogs.NAMESPACE + "." + permission);
    }
}
