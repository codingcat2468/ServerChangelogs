package com.codingcat.changelogs.lang;

import com.codingcat.changelogs.ServerChangelogs;
import io.papermc.paper.plugin.configuration.PluginMeta;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
public final class TranslationSource {
    private final @NotNull Path sourceDirectory;
    private final @NotNull PluginMeta pluginMeta;
    private final @NotNull ComponentLogger logger;
    private final Key storeKey = ServerChangelogs.KEY_GENERATOR.apply("translations");
    private final MiniMessage miniMessage = MiniMessage.builder()
            .tags(createGlobalTagResolver())
            .build();
    private @Nullable TranslationStore<?> translationStore;

    public void reload() {
        logger.info("Reloading translation store...");
        File[] files = sourceDirectory.toFile().listFiles(p -> p.getName().endsWith(".yml") || p.getName().endsWith(".yaml"));
        if (files == null) {
            logger.warn("Failed to find translation directory, no translations will be loaded!");
            return;
        }
        if (files.length == 0) {
            logger.warn("No translation files present, unable to load language!");
            return;
        }
        Map<String, Map<String, String>> rawTranslations = new HashMap<>();
        for (File file : files) {
            String withoutExtension = file.getName().substring(0, file.getName().lastIndexOf('.'));
            Map<String, String> data = this.loadTranslationFile(file.toPath());
            if (data == null) continue;
            rawTranslations.put(withoutExtension, data);
        }
        GlobalTranslator globalTranslator = GlobalTranslator.translator();
        if (this.translationStore != null) globalTranslator.removeSource(this.translationStore);
        this.translationStore = this.createTranslationStore(rawTranslations);
        globalTranslator.addSource(this.translationStore);
    }

    private @NotNull TranslationStore<?> createTranslationStore(@NotNull Map<String, Map<String, String>> rawLanguageData) {
        MiniMessageTranslationStore store = MiniMessageTranslationStore.create(this.storeKey, this.miniMessage);
        store.defaultLocale(Locale.US);
        for (Map.Entry<String, Map<String, String>> entry : rawLanguageData.entrySet()) {
            Locale locale;
            try {
                String key = entry.getKey().replace("-", "_");
                if (key.endsWith("_")) key = key.replace("_", "");
                locale = key.contains("_") ? Locale.of(
                        key.substring(0, key.indexOf('_')).toLowerCase(Locale.ROOT),
                        key.substring(key.indexOf("_") + 1).toUpperCase(Locale.ROOT)
                ) : Locale.of(key);
            } catch (Exception e) {
                logger.warn("Skipping unknown language entry \"{}\"", entry.getKey());
                continue;
            }
            store.registerAll(locale, entry.getValue());
        }
        return store;
    }

    private @Unmodifiable @Nullable Map<String, String> loadTranslationFile(@NotNull Path path) {
        File file = path.toFile();
        String fileName = path.getFileName().toString();
        if (!file.exists()) {
            logger.warn("Failed to find language file {}, skipping language!", fileName);
            return null;
        }
        YamlConfiguration langFile = new YamlConfiguration();
        try {
            langFile.load(file);
            //noinspection DataFlowIssue
            return langFile.getKeys(true)
                    .stream()
                    .filter(langFile::contains)
                    .filter(k -> !langFile.isConfigurationSection(k))
                    .collect(Collectors.toUnmodifiableMap(k -> ServerChangelogs.NAMESPACE + "." + k, langFile::getString));
        } catch (IOException e) {
            logger.warn("Failed to load language file {} due to I/O errors:", fileName, e);
        } catch (InvalidConfigurationException e) {
            logger.warn("Invalid syntax in language file {}:", fileName, e);
        }
        return null;
    }

    private @NotNull TagResolver createGlobalTagResolver() {
        return TagResolver.builder()
                .resolver(TagResolver.standard())
                .tag("prefix", Tag.inserting(translatable("prefix")))
                .tag("translate", (args, _) -> Tag.selfClosingInserting(translatable(args.popOr("Missing translate argument").value())))
                .tag("plugin", (args, context) -> {
                    String property = args.popOr("Missing argument for tag \"plugin\"").lowerValue();
                    String value = switch (property) {
                        case "name" -> pluginMeta.getName();
                        case "description" -> pluginMeta.getDescription();
                        case "version" -> pluginMeta.getVersion();
                        case "authors" -> String.join(", ", pluginMeta.getAuthors());
                        default -> throw context.newException("Invalid argument \"" + property + "\"", args);
                    };
                    return Tag.selfClosingInserting(text(Objects.requireNonNull(value)));
                }).build();
    }

    public static @NotNull TranslatableComponent translatable(@NotNull String key, @NotNull ComponentLike... args) {
        return Component.translatable(ServerChangelogs.NAMESPACE + "." + key, args);
    }

    /**
     * Used in dialogs specifically since paper doesn't provide {@link GlobalTranslator}
     * support for those yet (see <a href="https://github.com/PaperMC/Paper/issues/12971">this issue</a>)
     */
    public static @NotNull Component translatableManual(@NotNull Player player, @NotNull String key, @NotNull ComponentLike... args) {
        Component translation = GlobalTranslator.translator().translate(translatable(key, args), player.locale());
        return Objects.requireNonNullElseGet(translation, () -> text(key));
    }
}
