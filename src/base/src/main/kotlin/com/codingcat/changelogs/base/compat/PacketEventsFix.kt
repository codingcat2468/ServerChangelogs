package com.codingcat.changelogs.base.compat

import com.codingcat.changelogs.base.ServerChangelogs
import com.github.retrooper.packetevents.manager.server.ServerVersion
import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.component.builtin.item.FoodProperties
import com.github.retrooper.packetevents.protocol.dialog.body.ItemDialogBody
import com.github.retrooper.packetevents.util.PEVersion
import org.slf4j.kotlin.KLogger
import org.slf4j.kotlin.warn

private const val PE_MAJOR: Int = 2
private const val PE_MINOR: Int = 13
private const val PE_PATCH: Int = 1
private const val DUMMY_NUTRITION: Int = 123
private const val DUMMY_SATURATION_MODIFIER: Float = 123f

/**
 * This class introduces fixes for versions of PacketEvents that cause problems with the dialog system.
 */
object PacketEventsFix {
    private val enabledWorkarounds: MutableSet<Workaround> = mutableSetOf()

    enum class Workaround(
        private val versionPredicate: (PEVersion, ServerVersion) -> Boolean,
    ) {
        // https://github.com/retrooper/packetevents/issues/1569 - until v2.13.0 on MC versions < 26.1
        INVALID_ITEM_BODY_ENCODING(
            { p, s ->
                p.isOlderThan(PEVersion(PE_MAJOR, PE_MINOR, PE_PATCH)) && s.isOlderThan(ServerVersion.V_26_1)
            },
        );

        val isAffected: Boolean
            get() {
                val peVersion: PEVersion = ServerChangelogs.packetEventsApi.version
                val serverVersion: ServerVersion = ServerChangelogs.packetEventsApi.serverManager.version
                return this.versionPredicate(peVersion, serverVersion)
            }
    }

    fun setManualWorkarounds(manualWorkarounds: Set<Workaround>, logger: KLogger) {
        checkAndLoad(logger)
        enabledWorkarounds.addAll(manualWorkarounds)
        if (manualWorkarounds.isNotEmpty()) {
            logger.warn {
                buildString {
                    append("Manual workarounds for PacketEvents issues have been enabled. ")
                    append("This may cause unintended behavior if activated by accident!")
                }
            }
            val workaroundNames = manualWorkarounds.joinToString(", ") { it.name }
            logger.warn { "Enabled manual workarounds: ${workaroundNames}" }
        }
    }

    fun checkAndLoad(logger: KLogger) {
        enabledWorkarounds.clear()
        Workaround.entries.filterTo(enabledWorkarounds) { it.isAffected }
        if (enabledWorkarounds.isNotEmpty()) {
            logger.warn {
                buildString {
                    append("You are using a version of PacketEvents that contains known problems. ")
                    append("The plugin will automatically work around those issues, ")
                    append("but updating to the latest version is recommended!")
                }
            }
            val workaroundNames = enabledWorkarounds.joinToString(", ") { it.name }
            logger.warn { "Enabled workarounds: ${workaroundNames}" }
        }
    }

    fun isEnabled(workaround: Workaround): Boolean {
        return enabledWorkarounds.contains(workaround)
    }

    fun fixItemBody(itemBody: ItemDialogBody) {
        if (!isEnabled(Workaround.INVALID_ITEM_BODY_ENCODING)) return
        val item = itemBody.item
        if (item.hasComponentPatches()) return
        // Add dummy component data which won't affect the item visually
        item.setComponent(
            ComponentTypes.FOOD,
            FoodProperties(DUMMY_NUTRITION, DUMMY_SATURATION_MODIFIER, false),
        )
    }
}
