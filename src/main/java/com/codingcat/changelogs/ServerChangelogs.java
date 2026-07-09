package com.codingcat.changelogs;

import com.codingcat.changelogs.command.BrigadierCommandNode;
import com.codingcat.changelogs.command.DialogSubCommands;
import com.codingcat.changelogs.config.PluginConfig;
import com.codingcat.changelogs.data.ChangelogStorage;
import com.codingcat.changelogs.dialog.IDialog;
import com.codingcat.changelogs.event.ChangelogJoinListener;
import com.codingcat.changelogs.lang.TranslationSource;
import com.codingcat.changelogs.util.ResourceUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static com.codingcat.changelogs.command.BrigadierCommandNode.requirePermission;
import static com.codingcat.changelogs.lang.TranslationSource.translatable;
import static net.kyori.adventure.text.Component.text;

public final class ServerChangelogs extends JavaPlugin {
    public static @NotNull String NAMESPACE = "server_changelogs";
    @SuppressWarnings("PatternValidation")
    public static final Function<String, Key> KEY_GENERATOR = path -> Key.key(NAMESPACE, path);
    private static ComponentLogger logger;
    private TranslationSource translationSource;
    private PluginConfig config;
    private @Getter ChangelogStorage changelogStorage;
    private @Getter IDialog.Holder dialogHolder;

    @Override
    public void onEnable() {
        logger = this.getComponentLogger();
        Path translationPath = getDataPath().resolve("lang");
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
        this.translationSource = new TranslationSource(translationPath, getPluginMeta(), logger);
        this.translationSource.reload();
        info("console.startup");
        Path configPath = getDataPath().resolve("config.yml");
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
        this.dialogHolder = new IDialog.Holder(this);
        this.dialogHolder.recreate();
        getServer().getPluginManager().registerEvents(new ChangelogJoinListener(this::getChangelogStorage, dialogHolder), this);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralCommandNode<CommandSourceStack> rootNode = Commands.literal(NAMESPACE)
                    .requires(requirePermission("command"))
                    .executes(ctx -> {
                        ctx.getSource().getSender().sendMessage(translatable("command.root"));
                        return Command.SINGLE_SUCCESS;
                    }).build();
            BrigadierCommandNode.SUB_COMMANDS.forEach(c -> rootNode.addChild(c.build(this)));
            commands.registrar().register(rootNode, Set.of("changelogs", "scl"));
            if (this.config.registerDedicatedCommand())
                commands.registrar().register(DialogSubCommands.buildDedicatedChangelogCommand(this));
        });
    }

    public void reload() throws IOException, InvalidConfigurationException {
        info("console.reload");
        this.translationSource.reload();
        this.config.reload();
        String err = this.config.validate();
        if (err != null) throw new InvalidConfigurationException(err);
        this.changelogStorage.shutdown();
        this.changelogStorage = this.config.createChangelogStorage();
        info("console.startup_storage", text(this.changelogStorage.getDisplayName()));
        this.changelogStorage.init();
        this.dialogHolder.recreate();
    }

    @Override
    public void onDisable() {
        info("console.shutdown");
    }

    public @NotNull PluginConfig pluginConfig() {
        return this.config;
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
