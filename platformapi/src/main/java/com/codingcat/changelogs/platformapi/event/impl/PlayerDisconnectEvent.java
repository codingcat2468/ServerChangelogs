package com.codingcat.changelogs.platformapi.event.impl;

import com.codingcat.changelogs.platformapi.player.IPlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/** Fired when a player leaves, in either the configuration or the play phase. */
@Getter
@RequiredArgsConstructor
public final class PlayerDisconnectEvent implements IEvent {
    private final @NotNull IPlayer player;
}
