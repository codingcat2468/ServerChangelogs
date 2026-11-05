package com.codingcat.changelogs.platformapi.event.impl;

public sealed interface IEvent permits PlayerJoinEvent, PlayerEnterConfigurationPhaseEvent, PlayerDisconnectEvent {
}
