package com.codingcat.changelogs.base.config;

import com.codingcat.changelogs.base.ServerChangelogs;
import com.codingcat.changelogs.base.compat.PacketEventsFix;
import com.codingcat.changelogs.base.data.ChangelogStorage;
import com.codingcat.changelogs.base.dialog.DialogPackets;
import com.codingcat.changelogs.platformapi.item.NativeItemManager;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PluginConfig extends Yaml {
    private final @NotNull ServerChangelogs plugin;
    private final @NotNull Path path;
    private @NotNull Map<String, Object> data = new HashMap<>();

    public void tryReload() {
        try {
            this.reload();
        } catch (IOException | YAMLException e) {
            throw new RuntimeException("Failed to reload configuration", e);
        }
        String err = this.validate();
        if (err != null) throw new RuntimeException("Configuration invalid: " + err);
    }

    public void reload() throws IOException, YAMLException {
        this.data = this.load(new FileInputStream(this.path.toFile()));
    }

    public @Nullable String validate() {
        try {
            getDialogPacketPhase();
            createChangelogStorage();
            getDateFormatter();
            createChangelogHeaderStack();
            getEnabledManualWorkarounds();
            getPredefinedAuthors();
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

    public @NotNull DialogPackets.PacketPhase getDialogPacketPhase() {
        String rawPhase = getString("dialog_phase", DialogPackets.PacketPhase.PLAY.name());
        try {
            return DialogPackets.PacketPhase.valueOf(rawPhase.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid dialog phase \"" + rawPhase + "\"");
        }
    }

    public @NotNull Set<PacketEventsFix.Workaround> getEnabledManualWorkarounds() {
        List<String> rawWorkarounds = getList("enable_manual_workarounds", String.class);
        return rawWorkarounds.stream().map(workaround -> {
            try {
                return PacketEventsFix.Workaround.valueOf(workaround.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid manual workaround ID \"" + workaround + "\"");
            }
        }).collect(Collectors.toUnmodifiableSet());
    }

    public boolean showChangelogHeader() {
        return getBoolean("dialog_header", true);
    }

    public boolean useNativeFallbackPermissions() {
        return getBoolean("use_native_fallback_permissions", false);
    }

    public boolean useLegacyColorCodes() {
        return getBoolean("legacy_color_codes", true);
    }

    public @NotNull List<String> getPredefinedAuthors() {
        return getList("predefined_authors", String.class);
    }

    public @Nullable ItemStack createChangelogHeaderStack() {
        String value = getString("dialog_header_item", null);
        return value != null ? createStack(value) : null;
    }

    private String getString(@NotNull String key, @Nullable String defaultValue) {
        return (String) this.data.getOrDefault(key, defaultValue);
    }

    private boolean getBoolean(@NotNull String key, boolean defaultValue) {
        return (boolean) this.data.getOrDefault(key, defaultValue);
    }

    private <T> @NotNull List<T> getList(@NotNull String key, @NotNull Class<T> cls) {
        List<?> list = (List<?>) this.data.get(key);
        if (list == null) return List.of();
        return list.stream().map(cls::cast).toList();
    }

    @SuppressWarnings("PatternValidation")
    private @NotNull ItemStack createStack(@NotNull String input) {
        String idPart = input.contains("[") ? input.substring(0, input.indexOf('[')) : input;
        String componentPart = input.contains("[") ? input.substring(input.indexOf('[')) : null;
        Key inputKey;
        try {
            inputKey = Key.key(idPart);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid item ID \"" + idPart + "\"", e);
        }
        NativeItemManager nativeManager = this.plugin.getPlatform().getNativeItemManager();
        Object nativeStack = nativeManager.createNativeStack(inputKey, 1);
        Objects.requireNonNull(nativeStack, "Unknown item ID \"" + idPart + "\"");
        if (componentPart != null) nativeStack = nativeManager.applyComponentStr(nativeStack, componentPart);
        return (ItemStack) nativeManager.adaptToPEStack(nativeStack);
    }
}
