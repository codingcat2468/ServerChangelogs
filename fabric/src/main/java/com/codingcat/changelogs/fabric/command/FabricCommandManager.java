package com.codingcat.changelogs.fabric.command;

import com.codingcat.changelogs.fabric.player.FabricPlayerManager;
import com.codingcat.changelogs.platformapi.command.ICommandManager;
import com.codingcat.changelogs.platformapi.command.ICommandSource;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class FabricCommandManager implements ICommandManager {
    private record Registration(@NotNull LiteralCommandNode<CommandSourceStack> node, @NotNull Set<String> aliases) {
    }

    private final List<Registration> registrations = new ArrayList<>();
    private final @NotNull FabricPlayerManager playerManager;

    @Override
    public void register(@NotNull LiteralCommandNode<?> node, @NotNull Set<String> aliases) {
        //noinspection unchecked
        this.registrations.add(new Registration((LiteralCommandNode<CommandSourceStack>) node, aliases));
    }

    @Override
    public @NotNull ICommandSource adaptPlatformSource(@NotNull Object platformCommandSource) throws IllegalArgumentException {
        if (!(platformCommandSource instanceof CommandSourceStack stack))
            throw new IllegalArgumentException("Expected CommandSourceStack but got " + platformCommandSource);
        return new WrappedCommandSourceStack(stack, this.playerManager);
    }

    /** Must be called before the server creates its command dispatcher; registrations made later are still picked up. */
    public void registerHooks() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) ->
                this.registrations.forEach(r -> {
                    dispatcher.getRoot().addChild(r.node());
                    for (String alias : r.aliases()) {
                        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal(alias)
                                .requires(r.node().getRequirement())
                                .executes(r.node().getCommand())
                                .redirect(r.node()));
                    }
                }));
    }
}
