package com.codingcat.changelogs.base.lang

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlScalar
import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.platformapi.meta.ChangelogsMeta
import com.codingcat.changelogs.platformapi.meta.PlatformMeta
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationStore
import org.slf4j.kotlin.KLogger
import org.slf4j.kotlin.info
import org.slf4j.kotlin.warn
import java.io.IOException
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

/**
 * Manages translation stores and MiniMessage localization bundles loaded from YAML files.
 */
class TranslationSource(
    private val sourceDirectory: Path,
    private val changelogsMeta: ChangelogsMeta,
    private val platformMeta: PlatformMeta,
    private val logger: KLogger,
) {
    private val storeKey: Key by lazy { ServerChangelogs.key("translations") }
    private val miniMessage: MiniMessage by lazy {
        MiniMessage.builder()
            .tags(createGlobalTagResolver())
            .build()
    }
    private var translationStore: TranslationStore<*>? = null

    /**
     * Asynchronously reloads all translation files from disk on Dispatchers.IO.
     */
    suspend fun reloadAsync() = withContext(Dispatchers.IO) {
        this@TranslationSource.reload()
    }

    /**
     * Synchronously reloads translation stores from disk and registers them to Adventure GlobalTranslator.
     */
    fun reload() {
        logger.info { "Reloading translation store..." }
        if (!sourceDirectory.exists()) {
            logger.warn {
                buildString {
                    append("Failed to find translation directory, no translations will be loaded! ")
                    append("(Restart the server to re-generate the defaults!)")
                }
            }
            return
        }
        val files = sourceDirectory.listDirectoryEntries()
            .filter { it.name.endsWith(".yml") || it.name.endsWith(".yaml") }
        if (files.isEmpty()) {
            logger.warn {
                buildString {
                    append("No translation files present, unable to load language! ")
                    append("(Delete the \"lang\" directory and restart the server to re-generate the defaults!)")
                }
            }
            return
        }
        val rawTranslations = mutableMapOf<String, Map<String, String>>()
        for (file in files) {
            val withoutExtension = file.nameWithoutExtension
            val data = this.loadTranslationFile(file) ?: continue
            rawTranslations[withoutExtension] = data
        }
        val globalTranslator = GlobalTranslator.translator()
        this.translationStore?.let { globalTranslator.removeSource(it) }
        val newStore = this.createTranslationStore(rawTranslations)
        this.translationStore = newStore
        globalTranslator.addSource(newStore)
    }

    private fun createTranslationStore(
        rawLanguageData: Map<String, Map<String, String>>,
    ): TranslationStore<*> {
        val store = MiniMessageTranslationStore.create(this.storeKey, this.miniMessage)
        store.defaultLocale(Locale.US)
        for ((rawKey, value) in rawLanguageData) {
            val locale = runCatching {
                var key = rawKey.replace("-", "_")
                if (key.endsWith("_")) key = key.replace("_", "")
                if (key.contains("_")) {
                    Locale.of(
                        key.substring(0, key.indexOf('_')).lowercase(),
                        key.substring(key.indexOf("_") + 1).uppercase(),
                    )
                } else {
                    Locale.of(key)
                }
            }.getOrElse {
                logger.warn { "Skipping unknown language entry \"${rawKey}\"" }
                continue
            }
            store.registerAll(locale, value)
        }
        return store
    }

    private fun loadTranslationFile(path: Path): Map<String, String>? {
        val fileName = path.fileName.toString()
        if (!path.exists()) {
            logger.warn { "Failed to find language file ${fileName}, skipping language!" }
            return null
        }
        return try {
            val text = path.readText()
            val rootNode = Yaml.default.parseToYamlNode(text)
            val translations = mutableMapOf<String, String>()
            if (rootNode is YamlMap) {
                this.addRawTranslationsRecursive(translations, "${ServerChangelogs.NAMESPACE}.", rootNode)
            }
            translations
        } catch (e: IOException) {
            logger.warn(e) { "Failed to load language file ${fileName} due to I/O errors:" }
            null
        } catch (e: Exception) {
            logger.warn(e) { "Invalid syntax in language file ${fileName}:" }
            null
        }
    }

    private fun addRawTranslationsRecursive(
        rawTranslations: MutableMap<String, String>,
        prefix: String,
        map: YamlMap,
    ) {
        map.entries.forEach { (keyScalar, valueNode) ->
            val key = keyScalar.content
            when (valueNode) {
                is YamlScalar -> rawTranslations["${prefix}${key}"] = valueNode.content
                is YamlMap -> addRawTranslationsRecursive(
                    rawTranslations,
                    "${prefix}${key}.",
                    valueNode,
                )

                else -> {}
            }
        }
    }

    private fun createGlobalTagResolver(): TagResolver {
        return TagResolver.builder()
            .resolver(TagResolver.standard())
            .tag(
                "prefix",
                Tag.inserting(translatable("prefix")),
            )
            .tag("translate") { args, _ ->
                Tag.selfClosingInserting(
                    translatable(
                        args.popOr("Missing translate argument").value(),
                    ),
                )
            }
            .tag("plugin") { args, context ->
                val property = args.popOr("Missing argument for tag \"plugin\"").lowerValue()
                val value: String? = when (property) {
                    "name" -> changelogsMeta.name
                    "description" -> changelogsMeta.description
                    "version" -> changelogsMeta.version
                    "authors" -> changelogsMeta.authors?.joinToString(", ")
                    else -> throw context.newException("Invalid argument \"${property}\"", args)
                }
                Tag.selfClosingInserting(
                    value?.let(Component::text) ?: translatable("meta.unknown"),
                )
            }
            .tag("platform") { args, context ->
                val property = args.popOr("Missing argument for tag \"platform\"").lowerValue()
                val value: Component = when (property) {
                    "name" -> platformMeta.name
                    "version" -> Component.text(platformMeta.version)
                    else -> throw context.newException("Invalid argument \"${property}\"", args)
                }
                Tag.selfClosingInserting(value)
            }
            .build()
    }

    companion object {
        /**
         * Creates a namespaced [TranslatableComponent] formatted under this plugin's namespace.
         */
        fun translatable(
            key: String,
            vararg args: ComponentLike,
        ): TranslatableComponent {
            return Component.translatable(
                "${ServerChangelogs.NAMESPACE}.${key}",
                *args,
            )
        }

        /**
         * Used in dialogs specifically since paper doesn't provide [GlobalTranslator]
         * support for those yet (see [this issue](https://github.com/PaperMC/Paper/issues/12971))
         */
        fun translatableManual(
            player: PlatformPlayer,
            key: String,
            vararg args: ComponentLike,
        ): Component {
            return GlobalTranslator.render(
                translatable(key, *args),
                player.clientLocale,
            )
        }
    }
}
