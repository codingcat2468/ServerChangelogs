package com.codingcat.changelogs.platformapi.player;

import com.codingcat.changelogs.platformapi.audience.IBasePermissionHolderAudienceWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

public interface IPlayer extends IBasePermissionHolderAudienceWrapper {
    @NotNull UUID getUniqueId();

    @NotNull Locale getClientLocale();

    boolean isFirstJoin();

    @NotNull TriState isNativeAdmin();

    void kick(@Nullable Component reason);
}
