package com.codingcat.changelogs.fabric.player;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.util.TriState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public final class FabricPermissionUtil {
    private FabricPermissionUtil() {
    }

    public static @NotNull TriState convert(@NotNull net.fabricmc.fabric.api.util.TriState value) {
        return switch (value) {
            case TRUE -> TriState.TRUE;
            case FALSE -> TriState.FALSE;
            case DEFAULT -> TriState.NOT_SET;
        };
    }

    public static @NotNull PermissionChecker forEntity(@NotNull Entity entity) {
        return perm -> convert(Permissions.getPermissionValue(entity, perm));
    }

    public static @NotNull PermissionChecker forSource(@NotNull CommandSourceStack source) {
        // Console, RCON and command blocks have no entity and are always trusted, like on Paper
        if (source.getEntity() == null) return PermissionChecker.always(TriState.TRUE);
        return perm -> convert(Permissions.getPermissionValue(source, perm));
    }
}
