package com.codingcat.changelogs.base;

import com.codingcat.changelogs.api.ServerChangelogsApi;
import com.codingcat.changelogs.base.api.ServerChangelogsApiImpl;
import com.codingcat.changelogs.base.command.BrigadierCommandNode;
import com.codingcat.changelogs.base.command.DialogSubCommands;
import com.codingcat.changelogs.base.compat.PacketEventsFix;
import com.codingcat.changelogs.base.config.PluginConfig;
import com.codingcat.changelogs.base.data.ChangelogStorage;
import com.codingcat.changelogs.base.dialog.DialogSessionManager;
import com.codingcat.changelogs.base.dialog.IDialog;
import com.codingcat.changelogs.base.event.ChangelogJoinListener;
import com.codingcat.changelogs.base.lang.TranslationSource;
import com.codingcat.changelogs.base.util.ResourceUtil;
import com.codingcat.changelogs.platformapi.ChangelogsPlatform;
import com.codingcat.changelogs.platformapi.Entrypoint;
import com.codingcat.changelogs.platformapi.command.ICommandManager;
import com.codingcat.changelogs.platformapi.event.IEventManager;
import com.mojang.brigadier.tree.LiteralCommandNode;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.translation.GlobalTranslator;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static com.codingcat.changelogs.base.lang.TranslationSource.translatable;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static net.kyori.adventure.text.Component.text;

public final class ServerChangelogs extends Entrypoint {
    public static @NotNull String NAMESPACE = "server_changelogs";
    @SuppressWarnings("PatternValidation")
    public static final Function<String, Key> KEY_GENERATOR = path -> Key.key(NAMESPACE, path);
    private static ComponentLogger logger;
    private TranslationSource translationSource;
    private PluginConfig config;
    private ChangelogJoinListener joinListener;
    private @Getter ChangelogStorage changelogStorage;
    private @Getter IDialog.Holder dialogHolder;
    private @Getter DialogSessionManager dialogSessionManager;
    private ServerChangelogsApiImpl api;

    public ServerChangelogs(@NotNull ChangelogsPlatform platform) {
        super(platform);
    }

    @Override
    public void onStart() {
        logger = getPlatform().getComponentLogger();
        Path translationPath = getPlatform().getDataPath().resolve("lang");
        if (translationPath.toFile().mkdirs()) {
            logger.info("Creating default translation files...");
            ResourceUtil.readResourcesAsString("lang").forEach((fname, contents) -> {
                try {
                    Files.writeString(translationPath.resolve(fname), contents, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
                } catch (IOException e) {
                    logger.warn("Failed to create default translation file \"{}\":", fname, e);
                }
            });
        }
        this.translationSource = new TranslationSource(translationPath, getPlatform().getChangelogsMeta(), getPlatform().getPlatformMeta(), logger);
        this.translationSource.reload();
        info("console.startup");
        Path configPath = getPlatform().getDataPath().resolve("config.yml");
        if (!configPath.toFile().exists()) {
            logger.info("Creating default configuration file...");
            String defaultConfig = ResourceUtil.readResourceAsString("defaults/config.yml");
            try {
                Files.writeString(configPath, defaultConfig, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
            } catch (IOException e) {
                logger.warn("Failed to create default config file \"{}\":", configPath, e);
            }
        }
        this.config = new PluginConfig(this, configPath);
        this.config.tryReload();
        this.changelogStorage = this.config.createChangelogStorage();
        info("console.startup_storage", text(this.changelogStorage.getDisplayName()));
        this.changelogStorage.init();
        this.api = new ServerChangelogsApiImpl(this);
        PacketEventsFix.setManualWorkarounds(config.getEnabledManualWorkarounds(), logger);
        this.dialogHolder = new IDialog.Holder(this);
        this.dialogHolder.recreate();
        this.dialogSessionManager = new DialogSessionManager(this.dialogHolder, getPlatform().getPlayerManager());
        this.dialogSessionManager.registerEvents();
        this.joinListener = new ChangelogJoinListener(this::getChangelogStorage, dialogHolder, dialogSessionManager, config);
        this.joinListener.registerEvents(getPlatform().getEventManager());
        this.registerCommands(getPlatform().getCommandManager());
        this.api.enable();
    }

    private void registerCommands(@NotNull ICommandManager commandManager) {
        LiteralCommandNode<Object> rootNode = literal(NAMESPACE).build();
        BrigadierCommandNode.SUB_COMMANDS.forEach(c -> rootNode.addChild(c.build(this)));
        commandManager.register(rootNode, Set.of("changelogs", "scl"));
        if (this.config.registerDedicatedCommand())
            commandManager.register(DialogSubCommands.buildDedicatedChangelogCommand(this));
    }

    public void reload(boolean force) throws IOException, YAMLException, IDialog.DestroyRejectedException {
        info("console.reload");
        if (!force) this.dialogHolder.ensureCanReload();
        this.translationSource.reload();
        this.config.reload();
        String err = this.config.validate();
        if (err != null) throw new YAMLException(err);
        this.changelogStorage.shutdown();
        this.changelogStorage = this.config.createChangelogStorage();
        info("console.startup_storage", text(this.changelogStorage.getDisplayName()));
        this.changelogStorage.init();
        PacketEventsFix.setManualWorkarounds(config.getEnabledManualWorkarounds(), logger);
        this.dialogHolder.recreate();
        IEventManager eventManager = getPlatform().getEventManager();
        this.joinListener.unregisterEvents(eventManager);
        this.joinListener.registerEvents(eventManager);
    }

    @Override
    public void onShutdown() {
        info("console.shutdown");
        if (this.api != null) this.api.disable();
        if (this.dialogSessionManager != null) this.dialogSessionManager.unregisterEvents();
        if (this.joinListener != null) this.joinListener.unregisterEvents(getPlatform().getEventManager());
        if (this.changelogStorage != null) this.changelogStorage.shutdown();
    }

    public @NotNull PluginConfig pluginConfig() {
        return this.config;
    }

    public @NotNull ServerChangelogsApi getApi() {
        return this.api;
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
