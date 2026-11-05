package com.codingcat.changelogs.fabric.mixin;

import com.codingcat.changelogs.fabric.event.CustomClickHooks;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers the game and configuration listeners, as both inherit the handler from this class. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {
    @Shadow
    @Final
    protected MinecraftServer server;

    // Injected right before the vanilla dispatch, i.e. after the packet was moved onto the server thread
    @Inject(method = "handleCustomClickAction", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;handleCustomClickAction(Lnet/minecraft/resources/Identifier;Ljava/util/Optional;)V"))
    private void changelogs$onCustomClickAction(ServerboundCustomClickActionPacket packet, CallbackInfo ci) {
        CustomClickHooks.dispatch((ServerCommonPacketListenerImpl) (Object) this, this.server, packet);
    }
}
