package com.codingcat.changelogs.base

import com.codingcat.changelogs.base.command.DedicatedChangelogCommand
import com.codingcat.changelogs.base.command.ServerChangelogsCommand
import com.codingcat.changelogs.base.command.register
import com.codingcat.changelogs.base.compat.PacketEventsFix
import com.codingcat.changelogs.base.config.PluginConfig
import com.codingcat.changelogs.base.data.ChangelogStorage
import com.codingcat.changelogs.base.dialog.DialogSessionManager
import com.codingcat.changelogs.base.dialog.PluginDialog
import com.codingcat.changelogs.base.event.ChangelogJoinListener
import com.codingcat.changelogs.base.lang.TranslationSource
import com.codingcat.changelogs.base.lang.TranslationSource.Companion.translatable
import com.codingcat.changelogs.base.util.ResourceUtil
import com.codingcat.changelogs.platformapi.ChangelogsPlatform
import com.codingcat.changelogs.platformapi.Entrypoint
import com.codingcat.changelogs.platformapi.command.PlatformCommandManager
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.PacketEventsAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.kyori.adventure.translation.GlobalTranslator
import org.slf4j.kotlin.KLogger
import org.slf4j.kotlin.info
import org.slf4j.kotlin.warn
import java.io.IOException
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * Core entrypoint and central coordinator for ServerChangelogs plugin state, configuration, and services.
 */
object ServerChangelogs : Entrypoint {
    override lateinit var platform: ChangelogsPlatform

    override val packetEventsApi: PacketEventsAPI<out Any>
        get() = PacketEvents.getAPI()

    /**
     * Adventure key namespace for ServerChangelogs identifiers.
     */
    const val NAMESPACE: String = "server_changelogs"

    /**
     * Constructs a namespaced Adventure key for ServerChangelogs identifiers.
     */
    fun key(path: String): Key = Key.key(NAMESPACE, path)

    /**
     * Key generator function for namespaced Adventure keys.
     */
    @JvmField
    val KEY_GENERATOR: (String) -> Key = ::key

    /**
     * Active storage engine managing changelog persistence and reads.
     */
    lateinit var changelogStorage: ChangelogStorage
        private set

    /**
     * Container holding UI dialog definitions.
     */
    val dialogHolder: PluginDialog.Holder = PluginDialog.Holder

    /**
     * Session manager tracking active dialog interactions and packet events.
     */
    val dialogSessionManager: DialogSessionManager = DialogSessionManager

    private lateinit var translationSource: TranslationSource
    private lateinit var config: PluginConfig
    private lateinit var componentLogger: ComponentLogger
    lateinit var logger: KLogger
        private set

    override suspend fun onStart() {
        componentLogger = platform.getComponentLogger()
        logger = KLogger(componentLogger)

        val translationPath: Path = platform.getDataPath().resolve("lang")
        withContext(Dispatchers.IO) {
            if (!translationPath.exists()) {
                runCatching { translationPath.createDirectories() }
                    .onFailure { e -> logger.warn(e) { "Failed to create translation directory:" } }
                logger.info { "Creating default translation files..." }
                ResourceUtil.readResourcesAsString("lang").forEach { (fname, contents) ->
                    try {
                        translationPath.resolve(fname).writeText(contents)
                    } catch (e: IOException) {
                        logger.warn(e) { "Failed to create default translation file \"${fname}\":" }
                    }
                }
            }
        }
        this.translationSource = TranslationSource(
            translationPath,
            platform.changelogsMeta,
            platform.platformMeta,
            logger,
        )
        this.translationSource.reloadAsync()

        info("console.startup")

        val configPath: Path = platform.getDataPath().resolve("config.yml")
        withContext(Dispatchers.IO) {
            if (!configPath.exists()) {
                logger.info { "Creating default configuration file..." }
                val defaultConfig: String = ResourceUtil.readResourceAsString("defaults/config.yml")
                try {
                    configPath.writeText(defaultConfig)
                } catch (e: IOException) {
                    logger.warn(e) { "Failed to create default config file \"${configPath}\":" }
                }
            }
        }
        this.config = PluginConfig(configPath)
        this.config.tryReloadAsync()
        this.changelogStorage = this.config.createChangelogStorage()
        info(
            "console.startup_storage",
            Component.text(this.changelogStorage.displayName),
        )
        this.changelogStorage.initAsync()
        PacketEventsFix.setManualWorkarounds(config.enabledManualWorkarounds, logger)
        this.dialogHolder.recreate()
        this.dialogSessionManager.registerEvents()
        ChangelogJoinListener.registerEvents(platform.eventManager)
        this.registerCommands(platform.commandManager)
    }

    private fun registerCommands(commandManager: PlatformCommandManager) {
        commandManager.register(ServerChangelogsCommand, this)
        if (this.config.registerDedicatedCommand()) {
            commandManager.register(DedicatedChangelogCommand, this)
        }
    }

    /**
     * Reloads configuration, translations, dialogs, and changelog storage backends.
     *
     * @param force when true, forces dialog reloading even if active dialog editors reject dismissal.
     * @throws IOException if configuration or translation files fail to read.
     * @throws IllegalArgumentException if configuration syntax validation fails.
     * @throws PluginDialog.DestroyRejectedException if an active dialog rejects reload and [force] is false.
     */
    @Throws(IOException::class, PluginDialog.DestroyRejectedException::class)
    suspend fun reload(force: Boolean) {
        info("console.reload")
        if (!force) this.dialogHolder.ensureCanReload()
        this.translationSource.reloadAsync()
        this.config.reloadAsync()
        this.config.validate()?.let { err ->
            throw IllegalArgumentException(err)
        }
        this.changelogStorage.shutdownAsync()
        this.changelogStorage = this.config.createChangelogStorage()
        info(
            "console.startup_storage",
            Component.text(this.changelogStorage.displayName),
        )
        this.changelogStorage.initAsync()
        PacketEventsFix.setManualWorkarounds(config.enabledManualWorkarounds, logger)
        this.dialogHolder.recreate()
        val eventManager = platform.eventManager
        ChangelogJoinListener.unregisterEvents(eventManager)
        ChangelogJoinListener.registerEvents(eventManager)
    }

    override suspend fun onShutdown() {
        info("console.shutdown")
        this.dialogSessionManager.unregisterEvents()
        ChangelogJoinListener.unregisterEvents(platform.eventManager)
        this.changelogStorage.shutdownAsync()
    }

    /**
     * Active configuration settings instance.
     */
    fun pluginConfig(): PluginConfig {
        if (!::config.isInitialized) {
            val configPath: Path = runCatching { platform.getDataPath().resolve("config.yml") }
                .getOrElse { Path.of("config.yml") }
            this.config = PluginConfig(configPath)
            if (configPath.exists()) {
                this.config.reload()
            }
        }
        return this.config
    }

    fun setConfig(config: PluginConfig) {
        this.config = config
    }

    /**
     * Logs an informational message translated for the console locale.
     */
    fun info(key: String, vararg args: ComponentLike) {
        componentLogger.info(translateConsole(key, *args))
    }

    /**
     * Logs a warning message translated for the console locale.
     */
    fun warn(key: String, vararg args: ComponentLike) {
        componentLogger.warn(translateConsole(key, *args))
    }

    /**
     * Logs an error message and exception translated for the console locale.
     */
    fun error(key: String, e: Throwable, vararg args: ComponentLike) {
        componentLogger.error(translateConsole(key, *args), e)
    }

    private fun translateConsole(
        key: String,
        vararg args: ComponentLike,
    ): Component {
        val value: Component? = GlobalTranslator.translator()
            .translate(translatable(key, *args), Locale.US)
        return value ?: Component.text(key)
    }
}
