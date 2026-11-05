package com.codingcat.changelogs.platformapi.item;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NativeItemManager {
    @Nullable Object createNativeStack(@NotNull Key key, int amount);

    @NotNull Object applyComponentStr(@NotNull Object nativeStack, @NotNull String componentStr);
}
