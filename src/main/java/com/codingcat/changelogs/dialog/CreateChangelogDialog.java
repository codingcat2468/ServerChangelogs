package com.codingcat.changelogs.dialog;

import com.codingcat.changelogs.data.ChangelogEntry;
import com.codingcat.changelogs.data.ChangelogStorage;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static com.codingcat.changelogs.lang.TranslationSource.translatable;
import static com.codingcat.changelogs.lang.TranslationSource.translatableManual;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
@SuppressWarnings("UnstableApiUsage")
public class CreateChangelogDialog implements IDialog {
    private final @NotNull ChangelogStorage storage;

    @Override
    public @NotNull Dialog build(@NotNull Player p) {
        int newId = this.storage.nextUID() + 1;
        List<? extends DialogBody> body = List.of(
                DialogBody.item(ItemStack.of(Material.WRITABLE_BOOK),
                        DialogBody.plainMessage(translatableManual(p, "dialog.create.subtitle", text(newId))),
                        false, false, 10, 10)
        );
        List<? extends DialogInput> inputs = List.of(
                DialogInput.text("contents", 250, translatableManual(p, "dialog.create.input.contents"),
                        true, "", 20000, TextDialogInput.MultilineOptions.create(100, 300)),
                DialogInput.text("author", translatableManual(p, "dialog.create.input.author")).build()
        );
        return Dialog.create(b -> b.empty()
                .type(DialogType.confirmation(
                        ActionButton.builder(translatableManual(p, "dialog.create.button.publish")).action(action("publish")).width(160).build(),
                        ActionButton.builder(translatableManual(p, "dialog.create.button.cancel")).width(100).build()
                ))
                .base(DialogBase.builder(translatableManual(p, "dialog.create.title"))
                        .body(body).inputs(inputs)
                        .canCloseWithEscape(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .build()
                ));
    }

    @Override
    public void onActionTriggered(@NotNull String action, @NotNull DialogResponseView data, @NotNull Player source) {
        if (!action.equals("publish")) return;
        List<Component> lines = Objects.requireNonNull(data.getText("contents"))
                .lines()
                .map(MiniMessage.miniMessage()::deserialize)
                .toList();
        if (lines.isEmpty()) {
            source.sendMessage(translatable("dialog.create.error.content_empty"));
            return;
        }
        String author = Objects.requireNonNull(data.getText("author"));
        ChangelogEntry entry = new ChangelogEntry(
                this.storage.nextUID(),
                lines, Instant.now(),
                !author.isBlank() ? MiniMessage.miniMessage().deserialize(author) : null,
                new HashSet<>()
        );
        this.storage.storeEntry(entry);
        source.sendMessage(translatable("dialog.create.success"));
    }
}
