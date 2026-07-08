package com.codingcat.changelogs.dialog;

import com.codingcat.changelogs.data.ChangelogStorage;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.codingcat.changelogs.lang.TranslationSource.translatableManual;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
@SuppressWarnings("UnstableApiUsage")
public class CreateChangelogDialog implements IDialog {
    private final @NotNull ChangelogStorage storage;

    @Override
    public @NotNull Dialog build(@NotNull Player p) {
        int newId = this.storage.allocateUID() + 1;
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
                        ActionButton.builder(translatableManual(p, "dialog.create.button.publish")).width(160).build(),
                        ActionButton.builder(translatableManual(p, "dialog.create.button.cancel")).width(100).build()
                ))
                .base(DialogBase.builder(translatableManual(p, "dialog.create.title"))
                        .body(body).inputs(inputs)
                        .canCloseWithEscape(false)
                        .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                        .build()
                ));
    }
}
