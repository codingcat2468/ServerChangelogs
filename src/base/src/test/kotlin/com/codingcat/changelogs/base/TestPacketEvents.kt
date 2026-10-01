package com.codingcat.changelogs.base

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.PacketEventsAPI
import com.github.retrooper.packetevents.injector.ChannelInjector
import com.github.retrooper.packetevents.manager.player.PlayerManager
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager
import com.github.retrooper.packetevents.manager.server.ServerManager
import com.github.retrooper.packetevents.manager.server.ServerVersion
import com.github.retrooper.packetevents.netty.NettyManager
import com.github.retrooper.packetevents.settings.PacketEventsSettings
import com.github.retrooper.packetevents.util.PEVersion

object TestPacketEvents {
    fun setup(
        peVer: PEVersion = PEVersion(2, 14, 0),
        srvVer: ServerVersion = ServerVersion.V_1_21,
    ) {
        val testSettings = PacketEventsSettings().apply {
            customResourceProvider { path ->
                PacketEventsAPI::class.java.classLoader.getResourceAsStream(path)
            }
        }
        val api = object : PacketEventsAPI<Any>() {
            override fun isLoaded(): Boolean = true
            override fun init() {}
            override fun isInitialized(): Boolean = true
            override fun isTerminated(): Boolean = false
            override fun getPlugin(): Any = "dummy"
            override fun getSettings(): PacketEventsSettings = testSettings
            override fun getServerManager(): ServerManager = object : ServerManager {
                override fun getVersion(): ServerVersion = srvVer
            }

            override fun getProtocolManager(): ProtocolManager = throw UnsupportedOperationException()
            override fun getPlayerManager(): PlayerManager = throw UnsupportedOperationException()
            override fun getNettyManager(): NettyManager = io.github.retrooper.packetevents.netty.NettyManagerImpl()
            override fun getInjector(): ChannelInjector = throw UnsupportedOperationException()
            override fun getVersion(): PEVersion = peVer
        }
        PacketEvents.setAPI(api)
    }
}
