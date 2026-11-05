package com.codingcat.changelogs.fabric.item;

import com.codingcat.changelogs.platformapi.item.NativeItemManager;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.key.Key;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStackTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FabricNativeItemManager implements NativeItemManager {
    @Override
    public @Nullable Object createNativeStack(@NotNull Key key, int amount) {
        Identifier id = Identifier.tryParse(key.asString());
        if (id == null) return null;
        // Templates, not ItemStacks: item components are not bound yet while the mod initializes
        return BuiltInRegistries.ITEM.get(id).map(holder -> new ItemStackTemplate(holder, amount, DataComponentPatch.EMPTY)).orElse(null);
    }

    @Override
    public @NotNull Object applyComponentStr(@NotNull Object nativeStack, @NotNull String componentStr) throws IllegalArgumentException {
        if (!(nativeStack instanceof ItemStackTemplate stack))
            throw new IllegalArgumentException("Expected native ItemStackTemplate but got " + nativeStack);
        String id = BuiltInRegistries.ITEM.getKey(stack.item().value()).toString();
        try {
            // shortcut: resolves components against built-in registries, not the server's datapack ones, upgrade if configs need datapack enchantments etc.
            var input = new ItemParser(VanillaRegistries.createLookup()).parse(new StringReader(id + componentStr));
            return new ItemStackTemplate(input.item(), stack.count(), input.components());
        } catch (CommandSyntaxException e) {
            throw new IllegalArgumentException("Invalid item components \"" + componentStr + "\": " + e.getMessage(), e);
        }
    }
}
