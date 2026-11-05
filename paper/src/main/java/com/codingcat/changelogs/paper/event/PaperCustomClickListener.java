package com.codingcat.changelogs.paper.event;

import com.codingcat.changelogs.base.ServerChangelogs;
import com.codingcat.changelogs.paper.player.PaperConfigurationPhaseConnectionWrapper;
import com.codingcat.changelogs.paper.player.PaperPlayerManager;
import com.codingcat.changelogs.platformapi.player.IPlayer;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

/** Forwards dialog clicks (play and configuration phase) to the dialog session manager. */
@RequiredArgsConstructor
@SuppressWarnings("UnstableApiUsage")
public class PaperCustomClickListener implements Listener {
    private final @NotNull Supplier<ServerChangelogs> changelogs;
    private final @NotNull PaperPlayerManager playerManager;

    @EventHandler
    public void onCustomClick(@NotNull PlayerCustomClickEvent event) {
        IPlayer player = switch (event.getCommonConnection()) {
            case PlayerGameConnection game -> this.playerManager.fromNative(game.getPlayer());
            case PlayerConfigurationConnection config -> new PaperConfigurationPhaseConnectionWrapper(config);
            default -> null;
        };
        if (player == null) return;
        Optional<Tag> payload = Optional.empty();
        BinaryTagHolder holder = event.getTag();
        if (holder != null && !holder.string().isEmpty()) {
            try {
                payload = Optional.of(TagParser.parseCompoundFully(holder.string()));
            } catch (CommandSyntaxException _) {
                // Not a compound, which no handler of ours expects either way
            }
        }
        this.changelogs.get().getDialogSessionManager().handleCustomClick(
                Identifier.fromNamespaceAndPath(event.getIdentifier().namespace(), event.getIdentifier().value()), payload, player);
    }
}
