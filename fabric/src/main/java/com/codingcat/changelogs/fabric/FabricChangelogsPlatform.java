package com.codingcat.changelogs.fabric;

import com.codingcat.changelogs.base.Entrypoints;
import com.codingcat.changelogs.base.ServerChangelogs;
import com.codingcat.changelogs.fabric.event.CustomClickHooks;
import com.codingcat.changelogs.fabric.player.FabricConfigurationPhaseWrapper;
import com.codingcat.changelogs.fabric.command.FabricCommandManager;
import com.codingcat.changelogs.fabric.event.FabricEventManager;
import com.codingcat.changelogs.fabric.item.FabricNativeItemManager;
import com.codingcat.changelogs.fabric.meta.FabricChangelogsMeta;
import com.codingcat.changelogs.fabric.meta.FabricPlatformMeta;
import com.codingcat.changelogs.fabric.player.FabricPlayerManager;
import com.codingcat.changelogs.platformapi.ChangelogsPlatform;
import com.codingcat.changelogs.platformapi.Entrypoint;
import com.codingcat.changelogs.platformapi.player.IPlayer;
import lombok.Getter;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class FabricChangelogsPlatform implements DedicatedServerModInitializer, ChangelogsPlatform {
    public static final @NotNull String MOD_ID = "server_changelogs";

    private final @NotNull Entrypoint entrypoint = Entrypoints.create(this);
    private final @Getter ComponentLogger componentLogger = ComponentLogger.logger(MOD_ID);
    private final @Getter FabricPlatformMeta platformMeta = new FabricPlatformMeta();
    private final @Getter FabricChangelogsMeta changelogsMeta = new FabricChangelogsMeta();
    private final @Getter FabricNativeItemManager nativeItemManager = new FabricNativeItemManager();
    private final @Getter FabricPlayerManager playerManager = new FabricPlayerManager();
    private final @Getter FabricEventManager eventManager = new FabricEventManager(playerManager);
    private final @Getter FabricCommandManager commandManager = new FabricCommandManager(playerManager);

    @Override
    public void onInitializeServer() {
        // The MinecraftServer does not exist yet, but the command and event hooks below must be in place
        // before it is constructed, and the entrypoint registers its commands/listeners while starting.
        ServerLifecycleEvents.SERVER_STARTING.register(playerManager::setServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            try {
                CustomClickHooks.setHandler(null);
                this.entrypoint.onShutdown();
            } finally {
                this.eventManager.unregisterAll();
            }
        });
        this.commandManager.registerHooks();
        this.eventManager.registerHooks();
        try {
            this.entrypoint.onStart();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to start platform entrypoint", e);
        }
        CustomClickHooks.setHandler((listener, server, packet) -> {
            IPlayer player = switch (listener) {
                case ServerGamePacketListenerImpl game -> this.playerManager.fromNative(game.getPlayer());
                case ServerConfigurationPacketListenerImpl config -> new FabricConfigurationPhaseWrapper(config, server);
                default -> null;
            };
            if (player == null) return;
            try {
                ((ServerChangelogs) this.entrypoint).getDialogSessionManager().handleCustomClick(packet.id(), packet.payload(), player);
            } catch (Exception e) {
                // Must not propagate, or a faulty handler would disconnect the player
                this.componentLogger.error("Failed to handle custom click action {}", packet.id(), e);
            }
        });
    }

    @Override
    public @NotNull Path getDataPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("ServerChangelogs");
    }
}
