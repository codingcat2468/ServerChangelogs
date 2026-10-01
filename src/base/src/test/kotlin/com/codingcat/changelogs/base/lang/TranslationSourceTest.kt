package com.codingcat.changelogs.base.lang

import com.codingcat.changelogs.platformapi.meta.ChangelogsMeta
import com.codingcat.changelogs.platformapi.meta.PlatformMeta
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.util.TriState
import org.slf4j.LoggerFactory
import org.slf4j.kotlin.KLogger
import java.nio.file.Files
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertNotNull

class TranslationSourceTest {
    private val logger = KLogger(LoggerFactory.getLogger("TranslationSourceTest"))

    private class TestChangelogsMeta : ChangelogsMeta {
        override val name: String = "TestPlugin"
        override val version: String = "1.0.0"
        override val description: String = "A test plugin"
        override val authors: Set<String> = setOf("AuthorA", "AuthorB")
    }

    private class TestPlatformMeta : PlatformMeta {
        override val name: Component = Component.text("TestServer", NamedTextColor.AQUA)
        override val version: String = "1.21.4"
    }

    private class TestPlayer : PlatformPlayer {
        override val uniqueId: UUID = UUID.randomUUID()
        override val name: String = "TestPlayer"
        override val clientLocale: Locale = Locale.US
        override val isFirstJoin: Boolean = false
        override val isNativeAdmin: TriState = TriState.NOT_SET
        override fun asAudience(): Audience = Audience.empty()
        override fun asPermissionChecker(): PermissionChecker = PermissionChecker { TriState.TRUE }
        override fun kick(reason: Component?) {}
        override fun asPacketEventsUser(): Any = Unit
    }

    @Test
    fun testNonExistentAndEmptyDirectoryHandling() {
        val tempDir = Files.createTempDirectory("lang_test_empty")
        val nonExistent = tempDir.resolve("missing_lang")
        val source1 = TranslationSource(nonExistent, TestChangelogsMeta(), TestPlatformMeta(), logger)
        source1.reload() // Should not throw

        val emptyDir = tempDir.resolve("empty_lang").createDirectories()
        val source2 = TranslationSource(emptyDir, TestChangelogsMeta(), TestPlatformMeta(), logger)
        source2.reload() // Should not throw

        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun testLoadAndTranslate() {
        val tempDir = Files.createTempDirectory("lang_test_valid")
        val langFile = tempDir.resolve("en_us.yml")
        langFile.writeText(
            """
            prefix: "<gold>[Changelogs]</gold> "
            greeting: "<prefix>Hello <green>world</green>!"
            info:
              plugin_info: "<plugin:name> v<plugin:version> by <plugin:authors>"
              server_info: "Running on <platform:name> <platform:version>"
            trans_tag: "<translate:greeting>"
            meta:
              unknown: "Unknown"
            """.trimIndent(),
        )

        val source = TranslationSource(tempDir, TestChangelogsMeta(), TestPlatformMeta(), logger)
        source.reload()

        val renderedGreeting = GlobalTranslator.render(
            TranslationSource.translatable("greeting"),
            Locale.US,
        )
        assertNotNull(renderedGreeting)

        val player = TestPlayer()
        val manual = TranslationSource.translatableManual(player, "greeting")
        assertNotNull(manual)

        val pluginInfo = GlobalTranslator.render(
            TranslationSource.translatable("info.plugin_info"),
            Locale.US,
        )
        assertNotNull(pluginInfo)

        val serverInfo = GlobalTranslator.render(
            TranslationSource.translatable("info.server_info"),
            Locale.US,
        )
        assertNotNull(serverInfo)

        // Test reloadAsync
        runBlocking {
            source.reloadAsync()
        }

        tempDir.toFile().deleteRecursively()
    }
}
