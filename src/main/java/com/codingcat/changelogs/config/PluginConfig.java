package com.codingcat.changelogs.config;

import com.codingcat.changelogs.ServerChangelogs;
import com.codingcat.changelogs.data.ChangelogStorage;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@RequiredArgsConstructor
public class PluginConfig extends YamlConfiguration {
    private final @NotNull ServerChangelogs plugin;
    private final @NotNull Path path;

    public void tryReload() {
        try {
            this.reload();
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException("Failed to reload configuration", e);
        }
        String err = this.validate();
        if (err != null) throw new RuntimeException("Configuration invalid: " + err);
    }

    public void reload() throws IOException, InvalidConfigurationException {
        this.load(this.path.toFile());
    }

    public @Nullable String validate() {
        try {
            createChangelogStorage();
            getDateFormatter();
            createChangelogHeaderStack();
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public @NotNull ChangelogStorage createChangelogStorage() throws RuntimeException {
        return ChangelogStorage.create(getString("changelog_storage", "yaml"), this.plugin);
    }

    public @NotNull DateTimeFormatter getDateFormatter() {
        return DateTimeFormatter.ofPattern(getString("date_format", "----"))
                .withZone(ZoneId.of(getString("date_timezone", "UTC")));
    }

    public boolean registerDedicatedCommand() {
        return getBoolean("register_dedicated_command", true);
    }

    public boolean showChangelogHeader() {
        return getBoolean("dialog_header", true);
    }

    public @Nullable ItemStack createChangelogHeaderStack() {
        String value = getString("dialog_header_item");
        return value != null ? createStack(value) : null;
    }

    @SuppressWarnings("PatternValidation")
    private static @NotNull ItemStack createStack(@NotNull String input) {
        String idPart = input.contains("[") ? input.substring(0, input.indexOf('[')) : input;
        String componentPart = input.contains("[") ? input.substring(input.indexOf('[')) : null;
        Key inputKey;
        try {
            inputKey = Key.key(idPart);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid item ID \"" + idPart + "\"", e);
        }
        ItemType type = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).get(inputKey);
        Objects.requireNonNull(type, "Unknown item ID \"" + idPart + "\"");
        ItemStack stack = type.createItemStack();
        if (componentPart != null) {
            //noinspection deprecation
            stack = Bukkit.getUnsafe().modifyItemStack(stack, componentPart);
        }
        return stack;
    }
}
