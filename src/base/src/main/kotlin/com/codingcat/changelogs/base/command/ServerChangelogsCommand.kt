package com.codingcat.changelogs.base.command

import com.charleskorn.kaml.YamlException
import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.base.dialog.DialogPackets
import com.codingcat.changelogs.base.dialog.PluginDialog
import com.codingcat.changelogs.base.dialog.ui.ChangelogDialog
import com.codingcat.changelogs.base.dialog.ui.editor.ChangelogEditorDialog
import com.codingcat.changelogs.base.lang.TranslationSource
import com.codingcat.changelogs.platformapi.command.PlatformCommandSource
import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.kyori.adventure.text.Component
import java.io.IOException

/**
 * Root command for ServerChangelogs (`/server_changelogs`, `/changelogs`, `/scl`).
 *
 * Hosts all child subcommands (`create`, `view`, `reload`, `info`) configured within
 * the primary command construction block.
 *
 * @author GuavaDealer
 * @since Kotlin Migration
 */
object ServerChangelogsCommand : AbstractCommand(
    name = ServerChangelogs.NAMESPACE,
    description = "Root command for ServerChangelogs",
    aliases = listOf("changelogs", "scl"),
) {
    override fun buildCommand(builder: LiteralArgumentBuilder<Any>) {
        builder.executesSource { source, _ ->
            source.asAudience().sendMessage(TranslationSource.translatable("command.root"))
        }

        builder.literal("create") {
            requiresPermission("command.create", false)
            executesPlayer { player, _ ->
                this@ServerChangelogsCommand.plugin.dialogHolder.getFromType<ChangelogEditorDialog>()
                    .showTo(
                        player,
                        this@ServerChangelogsCommand.plugin.dialogSessionManager,
                        DialogPackets.PacketPhase.PLAY,
                    )
            }
        }

        builder.literal("view") {
            requiresPermission("command.view", true)
            executesPlayer { player, _ ->
                this@ServerChangelogsCommand.plugin.dialogHolder.getFromType<ChangelogDialog>()
                    .showTo(
                        player,
                        this@ServerChangelogsCommand.plugin.dialogSessionManager,
                        DialogPackets.PacketPhase.PLAY,
                    )
            }
        }

        builder.literal("reload") {
            requiresPermission("command.reload")
            executesSourceSuspend { source, _ ->
                this@ServerChangelogsCommand.executeReload(source, false)
            }
            literal("--force") {
                executesSourceSuspend { source, _ ->
                    this@ServerChangelogsCommand.executeReload(source, true)
                }
            }
        }

        builder.literal("info") {
            requiresPermission("command.info", true)
            executesSource { source, _ ->
                source.asAudience().sendMessage(TranslationSource.translatable("command.root"))
            }
        }
    }

    private suspend fun executeReload(source: PlatformCommandSource, force: Boolean): Int {
        val audience = source.asAudience()
        val time = System.currentTimeMillis()
        var errorMsg: Component? = null
        try {
            plugin.reload(force)
            val took = System.currentTimeMillis() - time
            audience.sendMessage(
                TranslationSource.translatable(
                    "command.reload.success",
                    Component.text(took),
                ),
            )
        } catch (e: IOException) {
            errorMsg = TranslationSource.translatable("command.reload.error.io")
            ServerChangelogs.error("command.reload.error.details", e)
        } catch (e: YamlException) {
            errorMsg = TranslationSource.translatable(
                "command.reload.error.invalid",
                Component.text(e.message),
            )
        } catch (e: IllegalArgumentException) {
            errorMsg = TranslationSource.translatable(
                "command.reload.error.invalid",
                Component.text(e.message ?: "Invalid configuration"),
            )
        } catch (e: PluginDialog.DestroyRejectedException) {
            errorMsg = TranslationSource.translatable("command.reload.error.dialog_reject.${e.key}")
        }
        errorMsg?.let(audience::sendMessage)
        return Command.SINGLE_SUCCESS
    }
}

/**
 * Dedicated root command (`/changelog`) displaying the changelog view dialog directly.
 */
object DedicatedChangelogCommand : AbstractCommand(
    name = "changelog",
    description = "Displays the server changelog dialog",
    permission = "dedicated_command",
    permissionEnabledByDefault = true,
) {
    override fun buildCommand(builder: LiteralArgumentBuilder<Any>) {
        builder.executesPlayer { player, _ ->
            plugin.dialogHolder.getFromType<ChangelogDialog>()
                .showTo(player, plugin.dialogSessionManager, DialogPackets.PacketPhase.PLAY)
        }
    }
}
