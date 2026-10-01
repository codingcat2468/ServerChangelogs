package com.codingcat.changelogs.base.compat

import com.codingcat.changelogs.platformapi.item.NativeItemManager
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.resources.ResourceLocation
import net.kyori.adventure.key.Key

object PacketEventsNativeItemManager : NativeItemManager {
    @Throws(IllegalArgumentException::class)
    override fun adaptToPEStack(nativeStack: Any): ItemStack {
        require(nativeStack is ItemStack) { "Expected PacketEvents ItemStack but got ${nativeStack}" }
        return nativeStack
    }

    override fun createNativeStack(key: Key, amount: Int): Any? {
        val type = ItemTypes.getRegistry().getByName(ResourceLocation(key))
        return type?.let { ItemStack.builder().type(it).amount(amount).build() }
    }

    override fun applyComponentStr(nativeStack: Any, componentStr: String): Any {
        // TODO: Implement component parsing
        return nativeStack
    }
}
