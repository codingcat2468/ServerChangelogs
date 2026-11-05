package com.codingcat.changelogs.fabric.mixin;

import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public interface ServerConfigurationPacketListenerImplAccessor {
    @Accessor("clientInformation")
    @Nullable ClientInformation changelogs$getClientInformation();
}
