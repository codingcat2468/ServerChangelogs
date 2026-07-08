package com.codingcat.changelogs;

import com.codingcat.changelogs.lang.TranslationSource;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

import static com.codingcat.changelogs.lang.TranslationSource.translatable;
import static net.kyori.adventure.text.Component.text;

public final class ServerChangelogs extends JavaPlugin {
    public static @NotNull String NAMESPACE = "server_changelogs";
    @SuppressWarnings("PatternValidation")
    public static final Function<String, Key> KEY_GENERATOR = path -> Key.key(NAMESPACE, path);
    private static ComponentLogger logger;
    private TranslationSource translationSource;

    @Override
    public void onEnable() {
        logger = this.getComponentLogger();
        Path translationPath = getDataPath().resolve("lang");
        if (!translationPath.toFile().exists()) //noinspection ResultOfMethodCallIgnored
            translationPath.toFile().mkdirs();
        this.translationSource = new TranslationSource(translationPath, getPluginMeta(), logger);
        this.translationSource.reload();
        info("console.startup");
    }

    public void reload() {
        info("console.reload");
        this.translationSource.reload();
    }

    @Override
    public void onDisable() {
        info("console.shutdown");
    }

    public static void info(@NotNull String key, @NotNull ComponentLike... args) {
        logger.info(translateConsole(key, args));
    }

    public static void warn(@NotNull String key, @NotNull ComponentLike... args) {
        logger.warn(translateConsole(key, args));
    }

    public static void error(@NotNull String key, @NotNull Throwable e, @NotNull ComponentLike... args) {
        logger.error(translateConsole(key, args), e);
    }

    private static @NotNull Component translateConsole(@NotNull String key, @NotNull ComponentLike... args) {
        Component value = GlobalTranslator.translator().translate(translatable(key, args), Locale.of("en_US"));
        return Objects.requireNonNullElseGet(value, () -> text(key));
    }
}
