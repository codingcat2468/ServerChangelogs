package com.codingcat.changelogs.base.compat

import com.codingcat.changelogs.base.TestPacketEvents
import com.github.retrooper.packetevents.manager.server.ServerVersion
import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.dialog.body.ItemDialogBody
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.util.PEVersion
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.slf4j.kotlin.KLogger
import kotlin.test.*

class PacketEventsFixTest {
    private val testLogger = KLogger(ComponentLogger.logger("PacketEventsFixTest"))

    @Test
    fun testWorkaroundDetectionWhenAffected() {
        TestPacketEvents.setup(PEVersion(2, 13, 0), ServerVersion.V_1_21)
        PacketEventsFix.checkAndLoad(testLogger)
        assertTrue(PacketEventsFix.isEnabled(PacketEventsFix.Workaround.INVALID_ITEM_BODY_ENCODING))
    }

    @Test
    fun testWorkaroundDetectionWhenNotAffected() {
        TestPacketEvents.setup(PEVersion(2, 14, 0), ServerVersion.V_1_21)
        PacketEventsFix.checkAndLoad(testLogger)
        assertFalse(PacketEventsFix.isEnabled(PacketEventsFix.Workaround.INVALID_ITEM_BODY_ENCODING))
    }

    @Test
    fun testSetManualWorkarounds() {
        TestPacketEvents.setup(PEVersion(2, 14, 0), ServerVersion.V_1_21)
        PacketEventsFix.setManualWorkarounds(setOf(PacketEventsFix.Workaround.INVALID_ITEM_BODY_ENCODING), testLogger)
        assertTrue(PacketEventsFix.isEnabled(PacketEventsFix.Workaround.INVALID_ITEM_BODY_ENCODING))

        PacketEventsFix.setManualWorkarounds(emptySet(), testLogger)
        assertFalse(PacketEventsFix.isEnabled(PacketEventsFix.Workaround.INVALID_ITEM_BODY_ENCODING))
    }

    @Test
    fun testFixItemBody() {
        TestPacketEvents.setup(PEVersion(2, 13, 0), ServerVersion.V_1_21)
        PacketEventsFix.checkAndLoad(testLogger)

        val item = ItemStack.builder().type(ItemTypes.PAPER).amount(1).build()
        val itemBody = ItemDialogBody(item, null, false, false, 1, 1)

        assertFalse(item.hasComponent(ComponentTypes.FOOD))
        PacketEventsFix.fixItemBody(itemBody)
        assertTrue(item.hasComponent(ComponentTypes.FOOD))

        // Calling fixItemBody again should return early because hasComponentPatches is true
        PacketEventsFix.fixItemBody(itemBody)
        assertTrue(item.hasComponent(ComponentTypes.FOOD))

        // Disable workaround by setting non-affected version and test that fixItemBody is no-op
        TestPacketEvents.setup(PEVersion(2, 14, 0), ServerVersion.V_1_21)
        PacketEventsFix.setManualWorkarounds(emptySet(), testLogger)
        val freshItem = ItemStack.builder().type(ItemTypes.STICK).amount(1).build()
        val freshItemBody = ItemDialogBody(freshItem, null, false, false, 1, 1)
        PacketEventsFix.fixItemBody(freshItemBody)
        assertFalse(freshItem.hasComponent(ComponentTypes.FOOD))
    }

    @Test
    fun testNativeItemManager() {
        val stack = ItemStack.builder().type(ItemTypes.BOOK).amount(2).build()
        val adapted = PacketEventsNativeItemManager.adaptToPEStack(stack)
        assertEquals(stack, adapted)

        assertFailsWith<IllegalArgumentException> {
            PacketEventsNativeItemManager.adaptToPEStack("not-a-stack")
        }

        val created = PacketEventsNativeItemManager.createNativeStack(Key.key("minecraft", "diamond"), 5)
        assertNotNull(created)
        assertTrue(created is ItemStack)
        assertEquals(5, created.amount)

        val nonExistent = PacketEventsNativeItemManager.createNativeStack(
            Key.key("minecraft", "non_existent_item_xyz"),
            1,
        )
        assertEquals(null, nonExistent)

        val applied = PacketEventsNativeItemManager.applyComponentStr(stack, "custom_components")
        assertEquals(stack, applied)
    }
}
