package com.codingcat.changelogs.data.storage;

import com.codingcat.changelogs.data.ChangelogEntry;
import com.codingcat.changelogs.data.ChangelogStorage;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiredArgsConstructor
public class YamlChangelogStorage implements ChangelogStorage {
    private final @NotNull Path filePath;
    private List<ChangelogEntry> cache;

    @Override
    public void init() {
        if (!filePath.toFile().exists()) {
            this.cache = new ArrayList<>();
            this.save();
            return;
        }
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.load(filePath.toFile());
            this.cache = this.deserializeEntries(config);
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException("Failed to load changelog data from " + filePath, e);
        }
    }

    @Override
    public void storeEntry(@NotNull ChangelogEntry entry) {
        this.cache.add(entry.uid(), entry);
        this.save();
    }

    @Override
    public boolean removeEntry(int uid) {
        boolean success = this.cache.removeIf(e -> e.uid() == uid);
        if (success) this.save();
        return success;
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> data = this.cache.stream()
                .map(this::serializeEntry)
                .toList();
        config.set("entries", data);
        try {
            config.save(filePath.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save changelog data to " + filePath, e);
        }
    }

    private @NotNull List<ChangelogEntry> deserializeEntries(@NotNull YamlConfiguration config) {
        try {
            List<?> entries = config.getList("entries");
            Objects.requireNonNull(entries, "entries");
            List<ChangelogEntry> results = new ArrayList<>();
            for (int i = 0; i < entries.size(); i++) {
                ConfigurationSection section = (ConfigurationSection) entries.get(i);
                results.add(this.deserializeEntry(i, section));
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize changelog entries", e);
        }
    }

    private @NotNull ChangelogEntry deserializeEntry(int uid, @NotNull ConfigurationSection section) {
        List<Component> lines = section.getStringList("serializedLines")
                .stream().map(GsonComponentSerializer.gson()::deserialize)
                .toList();
        String recordedAtRaw = Objects.requireNonNull(section.getString("recordedAt"), "recordedAt");
        Instant recordedAt = DateTimeFormatter.ISO_INSTANT.parse(recordedAtRaw, Instant::from);
        Component author = Optional.ofNullable(section.getString("author"))
                .map(GsonComponentSerializer.gson()::deserialize)
                .orElse(null);
        return new ChangelogEntry(uid, lines, recordedAt, author);
    }

    private @NotNull Map<String, Object> serializeEntry(@NotNull ChangelogEntry entry) {
        Map<String, Object> data = new HashMap<>();
        List<String> serializedLines = entry.lines()
                .stream().map(GsonComponentSerializer.gson()::serialize)
                .toList();
        data.put("serializedLines", serializedLines);
        String recordedAtRaw = DateTimeFormatter.ISO_INSTANT.format(entry.recordedAt());
        data.put("recordedAt", recordedAtRaw);
        String serializedAuthor = Optional.ofNullable(entry.author())
                .map(GsonComponentSerializer.gson()::serialize)
                .orElse(null);
        data.put("author", serializedAuthor);
        return data;
    }

    @Override
    public @NotNull List<ChangelogEntry> listEntries() {
        return this.cache;
    }

    @Override
    public int allocateUID() {
        return this.cache.size();
    }
}
