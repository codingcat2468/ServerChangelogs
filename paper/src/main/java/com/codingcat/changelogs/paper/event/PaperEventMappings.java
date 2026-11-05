package com.codingcat.changelogs.paper.event;

import com.codingcat.changelogs.paper.player.PaperConfigurationPhaseConnectionWrapper;
import com.codingcat.changelogs.paper.player.PaperPlayerManager;
import com.codingcat.changelogs.platformapi.event.impl.PlayerDisconnectEvent;
import com.codingcat.changelogs.platformapi.event.impl.PlayerEnterConfigurationPhaseEvent;
import com.codingcat.changelogs.platformapi.event.impl.PlayerJoinEvent;
import com.codingcat.changelogs.platformapi.event.util.PlatformEventMappings;
import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public final class PaperEventMappings extends PlatformEventMappings<Event> {
    PaperEventMappings(@NotNull PaperPlayerManager pm) {
        this.register(
                PlayerJoinEvent.class,
                org.bukkit.event.player.PlayerJoinEvent.class,
                ev -> new PlayerJoinEvent(pm.fromNative(ev.getPlayer()), true)
        );
        this.register(
                PlayerEnterConfigurationPhaseEvent.class,
                AsyncPlayerConnectionConfigureEvent.class,
                ev -> new PlayerEnterConfigurationPhaseEvent(new PaperConfigurationPhaseConnectionWrapper(ev.getConnection()), true)
        );
        this.register(
                PlayerDisconnectEvent.class,
                PlayerConnectionCloseEvent.class,
                ev -> new PlayerDisconnectEvent(ev.getPlayerUniqueId())
        );
    }
}
