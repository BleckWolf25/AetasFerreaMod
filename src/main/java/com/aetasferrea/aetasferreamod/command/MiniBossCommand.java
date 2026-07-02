/**
 * @file MiniBossCommand.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Spawns specific mini-bosses via `/aetasferrea miniboss spawn <type>`.
 *
 * @description
 * Registers a CommandDispatcher that adds the aetasferrea miniboss spawn subcommand, allowing ops to skip
 * progressive difficulty rules and directly spawn a boss at their feet for testing.
 *
 * @since 20/05/2026
 * @updated 01/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.command;

import java.util.List;

import com.aetasferrea.aetasferreamod.entity.boss.MiniBossManager;
import com.aetasferrea.aetasferreamod.world.MonarchWorldData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// ---------- CLASS: MINI BOSS COMMAND

public class MiniBossCommand {

    // ---------- MINI-BOSS TYPES
    private static final List<String> MINIBOSS_TYPES = List.of(
        "vigil", "castellan", "knight_unenchanted", "knight",
        "both_vigil_castellan", "knight_enchanted_and_double", "diamond_knight", "vanguard"
    );

    // ---------- BOSS TYPES
    private static final List<String> BOSS_TYPES = List.of(
        "monarch", "both"
    );

    // ---------- COMMAND REGISTRATION
    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("aetasferrea")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("miniboss")
                    .then(Commands.literal("spawn")
                        .then(Commands.argument("type", StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(MINIBOSS_TYPES, builder))
                            .executes(MiniBossCommand::spawnMiniboss)
                        )
                    )
                )
                .then(Commands.literal("boss")
                    .then(Commands.literal("spawn")
                        .then(Commands.argument("type", StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(BOSS_TYPES, builder))
                            .executes(MiniBossCommand::spawnBoss)
                        )
                    )
                )
                .then(Commands.literal("debug")
                    .then(Commands.literal("monarch")
                        .executes(MiniBossCommand::debugMonarch)
                    )
                )
        );
    }

    // ---------- COMMAND EXECUTION LOGIC
    @SuppressWarnings("null")
    private static int spawnMiniboss(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String type = StringArgumentType.getString(context, "type");
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        ServerPlayer player = source.getEntity() instanceof ServerPlayer sp ? sp : null;

        boolean success = MiniBossManager.spawnBossAtPosition(level, pos, type, player);

        if (success) {
            source.sendSuccess(() -> Component.translatable("message.aetasferreamod.command.spawned_miniboss", type), true);
            return 1;
        } else {
            source.sendFailure(Component.translatable("message.aetasferreamod.command.failed_miniboss"));
            return 0;
        }
    }

    @SuppressWarnings("null")
    private static int spawnBoss(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String type = StringArgumentType.getString(context, "type");
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        ServerPlayer player = source.getEntity() instanceof ServerPlayer sp ? sp : null;

        // Map "both" to "both_monarch_vanguard"
        String actualType = type.equals("both") ? "both_monarch_vanguard" : type;
        boolean success = MiniBossManager.spawnBossAtPosition(level, pos, actualType, player);

        if (success) {
            source.sendSuccess(() -> Component.literal("Successfully spawned boss: " + type), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to spawn boss. Unknown type or unsafe position."));
            return 0;
        }
    }

    @SuppressWarnings("null")
    private static int debugMonarch(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        
        source.sendSuccess(() -> Component.literal("=== Monarch Spawn Debug Info ==="), false);
        
        // Show current granted regions
        MonarchWorldData worldData = MonarchWorldData.get(level);
        source.sendSuccess(() -> Component.literal("Granted regions: " + worldData.toString()), false);
        
        // Show current biome
        var biomeName = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
            .getKey(level.getBiome(pos).value());
        source.sendSuccess(() -> Component.literal("Current biome: " + (biomeName != null ? biomeName.toString() : "unknown")), false);
        
        // Show nearby structures
        var structureRegistry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        source.sendSuccess(() -> Component.literal("Nearby structures:"), false);
        for (var structure : level.structureManager().getAllStructuresAt(pos).keySet()) {
            var structName = structureRegistry.getKey(structure);
            if (structName != null) {
                source.sendSuccess(() -> Component.literal("  - " + structName.toString()), false);
            }
        }
        
        // Show spawn position safety
        boolean solidFloor = level.getBlockState(pos.below()).isSolidRender(level, pos.below());
        boolean airAtPos = level.getBlockState(pos).isAir();
        boolean airAbove = level.getBlockState(pos.above()).isAir();
        source.sendSuccess(() -> Component.literal("Spawn position safety:"), false);
        source.sendSuccess(() -> Component.literal("  - Solid floor: " + solidFloor), false);
        source.sendSuccess(() -> Component.literal("  - Air at position: " + airAtPos), false);
        source.sendSuccess(() -> Component.literal("  - Air above: " + airAbove), false);
        source.sendSuccess(() -> Component.literal("  - Overall safe: " + (solidFloor && airAtPos && airAbove)), false);
        
        // Show dimension
        source.sendSuccess(() -> Component.literal("Dimension: " + level.dimension().location()), false);
        
        return 1;
    }
}
