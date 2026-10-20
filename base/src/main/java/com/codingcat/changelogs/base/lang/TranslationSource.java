package com.codingcat.changelogs.base.lang;

import com.codingcat.changelogs.base.ServerChangelogs;
import com.codingcat.changelogs.platformapi.meta.ChangelogsMeta;
import com.codingcat.changelogs.platformapi.meta.PlatformMeta;
import com.codingcat.changelogs.platformapi.player.IPlayer;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
public final class TranslationSource {
    private static final String FALLBACK_LANGUAGE_FILE = "en_US.yml";
    private final @NotNull Path sourceDirectory;
    private final @NotNull ChangelogsMeta changelogsMeta;
    private final @NotNull PlatformMeta platformMeta;
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
            logger.warn("Failed to find translation directory, no translations will be loaded! (Restart the server to re-generate the defaults!)");
            return;
        }
        if (files.length == 0) {
            logger.warn("No translation files present, unable to load language! (Delete the \"lang\" directory and restart the server to re-generate the defaults!)");
            return;
        }
        Map<String, Map<String, String>> rawTranslations = new HashMap<>();
        for (File file : files) {
            String withoutExtension = file.getName().substring(0, file.getName().lastIndexOf('.'));
            Map<String, String> data = this.loadTranslationFile(file.toPath());
            if (data == null) continue;
            this.addMissingKeys(file, data);
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
        Yaml langFile = new Yaml();
        try {
            Map<String, Object> data = langFile.load(new FileInputStream(file));
            Map<String, String> translations = new HashMap<>();
            this.addRawTranslationsRecursive(translations, ServerChangelogs.NAMESPACE + ".", data);
            return translations;
        } catch (IOException e) {
            logger.warn("Failed to load language file {} due to I/O errors:", fileName, e);
        } catch (YAMLException e) {
            logger.warn("Invalid syntax in language file {}:", fileName, e);
        }
        return null;
    }

    /**
     * Adds any keys that exist in the bundled defaults but are missing from the server's copy of the file, so
     * new translations show up after an update without the server owner having to delete their lang files.
     * Bundled languages are updated from their own built-in file, any other language from the built-in
     * {@value #FALLBACK_LANGUAGE_FILE} so it can be translated. Missing keys are inserted next to their siblings and
     * existing content is never modified.
     */
    private void addMissingKeys(@NotNull File file, @NotNull Map<String, String> translations) {
        String fileName = file.getName();
        String referenceText = this.loadBundledText(fileName);
        if (referenceText == null) referenceText = this.loadBundledText(FALLBACK_LANGUAGE_FILE);
        if (referenceText == null) return;
        Map<String, String> defaults = new HashMap<>();
        try {
            this.addRawTranslationsRecursive(defaults, ServerChangelogs.NAMESPACE + ".", new Yaml().load(referenceText));
        } catch (YAMLException e) {
            logger.warn("Failed to read bundled translations for {}:", fileName, e);
            return;
        }
        Map<String, String> missing = new LinkedHashMap<>();
        defaults.forEach((k, v) -> {
            if (!translations.containsKey(k)) missing.put(k, v);
        });
        if (missing.isEmpty()) return;
        try {
            String existing = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            String merged = LangFileMerger.merge(existing, referenceText);
            Map<String, String> stillMissing = new LinkedHashMap<>(missing);
            try {
                Map<String, String> mergedKeys = new HashMap<>();
                this.addRawTranslationsRecursive(mergedKeys, ServerChangelogs.NAMESPACE + ".", new Yaml().load(merged));
                stillMissing.keySet().removeIf(mergedKeys::containsKey);
            } catch (YAMLException e) {
                merged = existing;
            }
            if (merged.equals(existing)) stillMissing = new LinkedHashMap<>(missing);
            // Anything that couldn't be placed in the existing structure is appended as a dotted key instead
            if (!stillMissing.isEmpty()) merged = this.appendDotted(merged, stillMissing);
            Files.writeString(file.toPath(), merged, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warn("Failed to add missing translations to {}:", fileName, e);
        }
        logger.info("Added {} missing translation key(s) to language file {}", missing.size(), fileName);
        missing.forEach(translations::putIfAbsent);
    }

    private @NotNull String appendDotted(@NotNull String text, @NotNull Map<String, String> keys) {
        Map<String, Object> dotted = new LinkedHashMap<>();
        String namespacePrefix = ServerChangelogs.NAMESPACE + ".";
        keys.forEach((k, v) -> dotted.put(k.substring(namespacePrefix.length()), v));
        DumperOptions options = new DumperOptions();
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.DOUBLE_QUOTED);
        options.setWidth(Integer.MAX_VALUE);
        String separator = !text.isEmpty() && !text.endsWith("\n") ? "\n\n" : "\n";
        return text + separator + "# New translations added automatically by a plugin update, edit or translate them as needed:\n" + new Yaml(options).dump(dotted);
    }

    private @Nullable String loadBundledText(@NotNull String fileName) {
        try (InputStream stream = TranslationSource.class.getClassLoader().getResourceAsStream("lang/" + fileName)) {
            return stream == null ? null : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warn("Failed to read bundled translations {}:", fileName, e);
            return null;
        }
    }

    private void addRawTranslationsRecursive(@NotNull Map<String, String> rawTranslations, @NotNull String prefix, @NotNull Map<String, Object> data) {
        data.forEach((k, v) -> {
            if (v instanceof String str) rawTranslations.put(prefix + k, str);
            if (v instanceof Map<?, ?> map) //noinspection unchecked
                addRawTranslationsRecursive(rawTranslations, prefix + k + ".", (Map<String, Object>) map);
        });
    }

    private @NotNull TagResolver createGlobalTagResolver() {
        return TagResolver.builder()
                .resolver(TagResolver.standard())
                .tag("prefix", Tag.inserting(translatable("prefix")))
                .tag("translate", (args, _) -> Tag.selfClosingInserting(translatable(args.popOr("Missing translate argument").value())))
                .tag("plugin", (args, context) -> {
                    String property = args.popOr("Missing argument for tag \"plugin\"").lowerValue();
                    String value = switch (property) {
                        case "name" -> changelogsMeta.getName();
                        case "description" -> changelogsMeta.getDescription();
                        case "version" -> changelogsMeta.getVersion();
                        case "authors" -> Optional.ofNullable(changelogsMeta.getAuthors())
                                .map(a -> String.join(", ", a))
                                .orElse(null);
                        default -> throw context.newException("Invalid argument \"" + property + "\"", args);
                    };
                    return Tag.selfClosingInserting(value != null ? text(value) : translatable("meta.unknown"));
                })
                .tag("platform", (args, context) -> {
                    String property = args.popOr("Missing argument for tag \"platform\"").lowerValue();
                    Component value = switch (property) {
                        case "name" -> platformMeta.getName();
                        case "version" -> text(platformMeta.getVersion());
                        default -> throw context.newException("Invalid argument \"" + property + "\"", args);
                    };
                    return Tag.selfClosingInserting(value);
                }).build();
    }

    public static @NotNull TranslatableComponent translatable(@NotNull String key, @NotNull ComponentLike... args) {
        return Component.translatable(ServerChangelogs.NAMESPACE + "." + key, args);
    }

    /**
     * Used in dialogs specifically since paper doesn't provide {@link GlobalTranslator}
     * support for those yet (see <a href="https://github.com/PaperMC/Paper/issues/12971">this issue</a>)
     */
    public static @NotNull Component translatableManual(@NotNull IPlayer player, @NotNull String key, @NotNull ComponentLike... args) {
        Component translation = GlobalTranslator.render(translatable(key, args), player.getClientLocale());
        return Objects.requireNonNullElseGet(translation, () -> text(ServerChangelogs.NAMESPACE + "." + key));
    }
}
