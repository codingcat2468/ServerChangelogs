package com.codingcat.changelogs.dialog;

import com.codingcat.changelogs.data.ChangelogEntry;
import com.codingcat.changelogs.data.ChangelogStorage;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.codingcat.changelogs.lang.TranslationSource.translatable;
import static com.codingcat.changelogs.lang.TranslationSource.translatableManual;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
@SuppressWarnings("UnstableApiUsage")
public class ChangelogDialog implements IDialog {
    private final @NotNull ChangelogStorage storage;
    private final @NotNull DateTimeFormatter dateFormatter;
    private final boolean addHeader;
    private final @Nullable ItemStack headerItem;

    @Override
    public @NotNull Dialog build(@NotNull Player p) {
        Component authorNull = translatableManual(p, "dialog.changelog.unspecified_author");
        List<DialogBody> body = this.storage.listEntries()
                .reversed().stream()
                .map(e -> translatableManual(p, "dialog.changelog.entry" + (!e.hasRead(p) ? "_unread" : ""),
                        text(dateFormatter.format(e.recordedAt())), createLinesComponent(p, e), Objects.requireNonNullElse(e.author(), authorNull)))
                .map(c -> (DialogBody) DialogBody.plainMessage(c, 440))
                .toList();
        if (body.isEmpty()) body = List.of(DialogBody.plainMessage(translatableManual(p, "dialog.changelog.empty"), 350));
        if (this.addHeader) {
            body = new ArrayList<>(body);
            PlainMessageDialogBody headerBody = DialogBody.plainMessage(translatableManual(p, "dialog.changelog.header"), 260);
            body.addFirst(headerItem != null ? DialogBody.item(headerItem, headerBody, false, false, 17, 17) : headerBody);
        }
        final List<? extends DialogBody> finalBody = body;
        return Dialog.create(b -> b.empty()
                .type(DialogType.notice(
                        ActionButton.builder(translatableManual(p, "dialog.changelog.button.close")).width(60).action(action("close")).build()
                ))
                .base(DialogBase.builder(translatableManual(p, "dialog.changelog.title"))
                        .body(finalBody)
                        .canCloseWithEscape(true)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .build()
                ));
    }

    private @NotNull Component createLinesComponent(@NotNull Player player, @NotNull ChangelogEntry entry) {
        Component component = Component.empty();
        List<Component> newLines = entry.lines().stream()
                .map(l -> translatableManual(player, "dialog.changelog.entry_line", l))
                .toList();
        for (Component line : newLines) {
            component = component.append(line);
            if (newLines.indexOf(line) < newLines.size() - 1) component = component.appendNewline();
        }
        return component;
    }

    @Override
    public void onActionTriggered(@NotNull String action, @NotNull DialogResponseView data, @NotNull Player source) {
        if (!action.equals("close")) return;
        List<Integer> uids = this.storage.listEntries()
                .stream()
                .filter(e -> !e.hasRead(source))
                .map(ChangelogEntry::uid)
                .toList();
        uids.forEach(uid -> storage.markAsRead(uid, source.getUniqueId()));
        if (!uids.isEmpty()) source.sendMessage(translatable("dialog.changelog.read", text(uids.size())));
    }
}
