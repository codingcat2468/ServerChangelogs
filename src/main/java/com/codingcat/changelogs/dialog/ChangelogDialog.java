package com.codingcat.changelogs.dialog;

import com.codingcat.changelogs.data.ChangelogEntry;
import com.codingcat.changelogs.data.ChangelogStorage;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static com.codingcat.changelogs.lang.TranslationSource.translatableManual;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
@SuppressWarnings("UnstableApiUsage")
public class ChangelogDialog implements IDialog {
    private final @NotNull ChangelogStorage storage;
    private final @NotNull DateTimeFormatter dateFormatter;

    @Override
    public @NotNull Dialog build(@NotNull Player p) {
        Component authorNull = translatableManual(p, "dialog.changelog.unspecified_author");
        List<? extends DialogBody> body = this.storage.listEntries()
                .reversed().stream()
                .map(e -> translatableManual(p, "dialog.changelog.entry",
                        text(dateFormatter.format(e.recordedAt())), createLinesComponent(p, e), Objects.requireNonNullElse(e.author(), authorNull)))
                .map(c -> DialogBody.plainMessage(c, 440))
                .toList();
        return Dialog.create(b -> b.empty()
                .type(DialogType.notice(
                        ActionButton.builder(translatableManual(p, "dialog.changelog.button.close")).width(60).build()
                ))
                .base(DialogBase.builder(translatableManual(p, "dialog.changelog.title"))
                        .body(body)
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
    }
}
