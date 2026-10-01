package com.codingcat.changelogs.platformapi.item

import com.github.retrooper.packetevents.protocol.item.ItemStack
import net.kyori.adventure.key.Key

/**
 * Abstraction bridging platform-native item stack representations with PacketEvents item stacks.
 */
interface NativeItemManager {
    /**
     * Converts a platform-native item stack into a PacketEvents [ItemStack].
     */
    fun adaptToPEStack(nativeStack: Any): ItemStack

    /**
     * Constructs a native item stack for the item identifier [key] with the specified [amount].
     * Returns `null` if the item type is unknown to the underlying platform.
     */
    fun createNativeStack(key: Key, amount: Int): Any?

    /**
     * Parses and applies a SNBT or JSON component string to the target native item stack.
     */
    fun applyComponentStr(nativeStack: Any, componentStr: String): Any
}
