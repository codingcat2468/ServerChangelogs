package com.codingcat.changelogs.base.dialog;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.chat.ComponentSerialization;
import org.jetbrains.annotations.NotNull;

public final class NativeComponents {
    private NativeComponents() {
    }

    /** Converts an Adventure component to a vanilla one through its JSON form, which does not need a server instance. */
    public static net.minecraft.network.chat.@NotNull Component toNative(@NotNull Component component) {
        JsonElement json = GsonComponentSerializer.gson().serializeToTree(component);
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(err -> new IllegalStateException("Unable to convert component to vanilla: " + err));
    }
}
