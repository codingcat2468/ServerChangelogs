package com.codingcat.changelogs.base.dialog.ui.editor;

import com.codingcat.changelogs.base.ServerChangelogs;
import com.codingcat.changelogs.base.data.ChangelogStorage;
import com.codingcat.changelogs.base.dialog.DialogPackets;
import com.codingcat.changelogs.base.dialog.DialogSessionManager;
import com.codingcat.changelogs.base.dialog.IDialog;
import com.codingcat.changelogs.base.dialog.ui.ChangelogDialog;
import com.codingcat.changelogs.platformapi.player.IPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.ItemBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.codingcat.changelogs.base.dialog.NativeComponents.toNative;
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
            Component edit = null;
            if (!isEditing) edit = translatableManual(p, prefix + "format", translatable(prefix + "edit"))
                    .decoration(TextDecoration.BOLD, false)
                    .clickEvent(sessionManager.createSessionBasedClickEvent(this, "start_edit_line", "target_line", idx));
            Component remove = translatableManual(p, prefix + "format", translatable(prefix + "remove"))
                    .decoration(TextDecoration.BOLD, false)
                    .clickEvent(sessionManager.createSessionBasedClickEvent(this, "remove_line", "target_line", idx));
            Component finalCmp = line.appendSpace();
            if (!isEditing) finalCmp = finalCmp.append(edit).appendSpace();
            return finalCmp.append(remove);
        };
        ItemBody itemBody = new ItemBody(new ItemStackTemplate(Items.WRITABLE_BOOK),
                Optional.of(new PlainMessage(toNative(translatableManual(p, sessionTranslation + ".subtitle", text(session.getEntryUID() + 1))), 160)),
                false, false, 15, 15);
        List<DialogBody> body = List.of(itemBody,
                new PlainMessage(toNative(translatableManual(p, "dialog.editor.hint")), ChangelogDialog.LINE_WIDTH),
                new PlainMessage(toNative(!previewLines.isEmpty() ? createLinesComponent(p, lineMapper, previewLines) : translatableManual(p, "dialog.editor.empty_preview")), ChangelogDialog.LINE_WIDTH)
        );
        if (session.isShowRestoredMessage()) {
            body = new ArrayList<>(body);
            body.add(1, new PlainMessage(toNative(translatableManual(p, "dialog.editor.restored_session")), 400));
        }
        List<Input> inputs = List.of(
                new Input("line", new TextInput(350, toNative(translatableManual(p, "dialog.editor.input.contents")),
                        true, session.getCurrentLine(), 5000, Optional.empty())),
                new Input("author", new TextInput(200, toNative(translatableManual(p, "dialog.editor.input.author")), true, session.getAuthor(), 200, Optional.empty()))
        );
        CommonDialogData common = new CommonDialogData(
                toNative(translatableManual(p, TITLE_KEY.apply(session))),
                Optional.empty(), true, false,
                DialogAction.NONE, body, inputs
        );
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(new ActionButton(new CommonButtonData(toNative(translatableManual(p, sessionTranslation + ".commit_button")), Optional.empty(), 160), Optional.of(sessionManager.createSessionBasedAction(this, "commit", true))));
        String lineAction = session.getEditingLineIndex() != -1 ? "edit_line" : "add_line";
        buttons.add(new ActionButton(new CommonButtonData(toNative(translatableManual(p, "dialog.editor.button." + lineAction)), Optional.empty(), 100), Optional.of(sessionManager.createSessionBasedAction(this, lineAction, true))));
        ActionButton closeBtn = new ActionButton(new CommonButtonData(toNative(translatableManual(p, "dialog.editor.button.close")), Optional.empty(), 100), Optional.of(sessionManager.createSessionBasedAction(this, "try_close", true)));
        return new MultiActionDialog(common, buttons, Optional.of(closeBtn), 3);
    }

    private @NotNull Dialog buildConfirmCloseDialog(@NotNull IPlayer p, @NotNull DialogSessionManager sessionManager) {
        EditorSession session = sessionManager.getSessionData(p, EditorSession.class);
        String prefix = "dialog.editor.confirm_close.";
        CommonDialogData common = DialogPackets.createSimpleDialog(p, TITLE_KEY.apply(session), prefix + "content", false, false);
        Function<Boolean, Action> closeActionFunc = save -> {
            CompoundTag payload = new CompoundTag();
            payload.putBoolean("save_session", save);
            return sessionManager.createSessionBasedAction(this, "close", payload, false);
        };
        ActionButton saveSession = new ActionButton(new CommonButtonData(toNative(translatableManual(p, prefix + "button.save_session")), Optional.empty(), 140), Optional.of(closeActionFunc.apply(true)));
        ActionButton discardSession = new ActionButton(new CommonButtonData(toNative(translatableManual(p, prefix + "button.discard_session")), Optional.empty(), 140), Optional.of(closeActionFunc.apply(false)));
        ActionButton cancel = new ActionButton(new CommonButtonData(toNative(translatableManual(p, prefix + "button.cancel")), Optional.empty(), 100), Optional.of(sessionManager.createSessionBasedAction(this, "reopen", false)));
        return new MultiActionDialog(common, List.of(saveSession, discardSession, cancel), Optional.empty(), 3);
    }

    private void actuallyClose(@NotNull IPlayer source, @NotNull DialogSessionManager sessionManager) {
        sessionManager.endSession(source);
        DialogPackets.clearDialog(source, DialogPackets.PacketPhase.PLAY);
    }

    @Override
    public void onActionTriggered(@NotNull String action, @Nullable CompoundTag data, @NotNull IPlayer source, @NotNull DialogSessionManager sessionManager) {
        EditorSession session = sessionManager.getSessionData(source, EditorSession.class);
        session.setShowRestoredMessage(false);
        if (data != null && data.contains("line")) {
            String rawLine = DialogSessionManager.requireString(data, "line");
            String rawAuthor = DialogSessionManager.requireString(data, "author");
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
                boolean saveSession = DialogSessionManager.requireBoolean(data, "save_session");
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
            case "start_edit_line", "remove_line" -> {
                if (data == null) return;
                int lineIdx = DialogSessionManager.requireInt(data, "target_line");
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
