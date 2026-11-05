package com.codingcat.changelogs.paper.command;

import com.codingcat.changelogs.paper.player.PaperPlayerManager;
import com.codingcat.changelogs.platformapi.command.ICommandManager;
import com.codingcat.changelogs.platformapi.command.ICommandSource;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class PaperCommandManager implements ICommandManager {
    private final Set<Consumer<Commands>> registrarActions = new HashSet<>();
    private PaperPlayerManager playerManager;

    public void supplyPlayerManager(@NotNull PaperPlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @Override
    public void register(@NotNull LiteralCommandNode<?> node, @NotNull Set<String> aliases) {
        //noinspection unchecked
        LiteralCommandNode<CommandSourceStack> node1 = (LiteralCommandNode<CommandSourceStack>) node;
        this.registrarActions.add(c -> c.register(node1, aliases));
    }

    @Override
    public @NotNull ICommandSource adaptPlatformSource(@NotNull Object platformCommandSource) throws IllegalArgumentException {
        if (!(platformCommandSource instanceof CommandSourceStack sourceStack))
            throw new IllegalArgumentException("Expected native paper CommandSourceStack but got " + platformCommandSource);
        return new WrappedCommandSourceStack(sourceStack, this.playerManager);
    }

    public void registerEvents(@NotNull LifecycleEventManager<Plugin> lifecycleEventManager) {
        lifecycleEventManager.registerEventHandler(LifecycleEvents.COMMANDS, commands ->
                this.registrarActions.forEach(a -> a.accept(commands.registrar())));
    }
}
