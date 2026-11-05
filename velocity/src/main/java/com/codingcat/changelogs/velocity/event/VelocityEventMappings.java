package com.codingcat.changelogs.velocity.event;

import com.codingcat.changelogs.platformapi.event.impl.PlayerDisconnectEvent;
import com.codingcat.changelogs.platformapi.event.impl.PlayerEnterConfigurationPhaseEvent;
import com.codingcat.changelogs.platformapi.event.impl.PlayerJoinEvent;
import com.codingcat.changelogs.platformapi.event.util.PlatformEventMappings;
import com.codingcat.changelogs.velocity.player.VelocityPlayerManager;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.configuration.PlayerConfigurationEvent;
import org.jetbrains.annotations.NotNull;

public class VelocityEventMappings extends PlatformEventMappings<Object> {
    VelocityEventMappings(@NotNull VelocityPlayerManager playerManager) {
        this.register(
                PlayerJoinEvent.class,
                ServerPostConnectEvent.class,
                e -> new PlayerJoinEvent(playerManager.fromNative(e.getPlayer()), e.getPreviousServer() == null)
        );
        this.register(
                PlayerEnterConfigurationPhaseEvent.class,
                PlayerConfigurationEvent.class,
                e -> new PlayerEnterConfigurationPhaseEvent(playerManager.fromNative(e.player()), e.server().getPreviousServer().isEmpty())
        );
        this.register(
                PlayerDisconnectEvent.class,
                DisconnectEvent.class,
                e -> new PlayerDisconnectEvent(e.getPlayer().getUniqueId())
        );
    }
}
