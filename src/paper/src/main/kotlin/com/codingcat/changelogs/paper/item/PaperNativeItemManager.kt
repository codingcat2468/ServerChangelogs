package com.codingcat.changelogs.paper.item

import com.codingcat.changelogs.platformapi.item.NativeItemManager
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import com.github.retrooper.packetevents.protocol.item.ItemStack as PEItemStack

object PaperNativeItemManager : NativeItemManager {
    @Throws(IllegalArgumentException::class)
    override fun adaptToPEStack(nativeStack: Any): PEItemStack {
        require(nativeStack is ItemStack) { "Expected native bukkit ItemStack but got ${nativeStack}" }
        return SpigotConversionUtil.fromBukkitItemStack(nativeStack)
    }

    override fun createNativeStack(key: Key, amount: Int): Any? {
        val type: ItemType? = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).get(key)
        return type?.createItemStack(amount)
    }

    @Throws(IllegalArgumentException::class)
    override fun applyComponentStr(nativeStack: Any, componentStr: String): Any {
        require(nativeStack is ItemStack) { "Expected native bukkit ItemStack but got ${nativeStack}" }
        val itemKey = nativeStack.type.key.asString()
        val created = Bukkit.getItemFactory().createItemStack(
            buildString {
                append(itemKey)
                append(componentStr)
            },
        )
        created.amount = nativeStack.amount
        return created
    }
}
