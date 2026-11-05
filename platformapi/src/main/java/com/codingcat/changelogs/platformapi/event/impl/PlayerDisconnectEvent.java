package com.codingcat.changelogs.platformapi.event.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired when a player's connection closes, in any phase. Only the UUID is available, since some platforms
 * no longer have a player object at this point (e.g. when leaving during the configuration phase).
 */
@Getter
@RequiredArgsConstructor
public final class PlayerDisconnectEvent implements IEvent {
    private final @NotNull UUID playerId;
}
