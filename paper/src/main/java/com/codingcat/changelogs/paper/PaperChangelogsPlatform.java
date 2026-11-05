package com.codingcat.changelogs.paper;

import com.codingcat.changelogs.base.Entrypoints;
import com.codingcat.changelogs.paper.command.PaperCommandManager;
import com.codingcat.changelogs.paper.event.PaperEventManager;
import com.codingcat.changelogs.base.ServerChangelogs;
import com.codingcat.changelogs.base.item.VanillaNativeItemManager;
import com.codingcat.changelogs.paper.event.PaperCustomClickListener;
import com.codingcat.changelogs.paper.meta.PaperPlatformMeta;
import com.codingcat.changelogs.paper.meta.PaperPluginChangelogsMeta;
import com.codingcat.changelogs.paper.player.PaperPlayerManager;
import com.codingcat.changelogs.platformapi.ChangelogsPlatform;
import com.codingcat.changelogs.platformapi.Entrypoint;
import lombok.Getter;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class PaperChangelogsPlatform extends JavaPlugin implements ChangelogsPlatform {
    private final @NotNull Entrypoint entrypoint = Entrypoints.create(this);
    private final @Getter PaperPlatformMeta platformMeta = new PaperPlatformMeta(getServer());
    private final @Getter PaperPluginChangelogsMeta changelogsMeta = new PaperPluginChangelogsMeta(getPluginMeta());
    private final @Getter VanillaNativeItemManager nativeItemManager = new VanillaNativeItemManager();
    private final @Getter PaperPlayerManager playerManager = new PaperPlayerManager(getServer());
    private final @Getter PaperEventManager eventManager = new PaperEventManager(this, getServer().getPluginManager());
    private final @Getter PaperCommandManager commandManager = new PaperCommandManager();
    private boolean earlyInitSucceeded = false;

    @Override
    public void onEnable() {
        this.commandManager.supplyPlayerManager(getPlayerManager());
        this.eventManager.initMappings(getPlayerManager());
        this.earlyInitSucceeded = true;
        try {
            this.entrypoint.onStart();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to start platform entrypoint", e);
        }
        getServer().getPluginManager().registerEvents(new PaperCustomClickListener(() -> (ServerChangelogs) this.entrypoint, this.playerManager), this);
        this.commandManager.registerEvents(getLifecycleManager());
    }

    @Override
    public void onDisable() {
        if (!this.earlyInitSucceeded) return;
        try {
            this.entrypoint.onShutdown();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to shut down platform entrypoint", e);
        } finally {
            this.eventManager.unregisterAll();
        }
    }

    @Override
    public @NotNull Path getDataPath() {
        return super.getDataPath();
    }

    @Override
    public @NotNull ComponentLogger getComponentLogger() {
        return super.getComponentLogger();
    }
}
