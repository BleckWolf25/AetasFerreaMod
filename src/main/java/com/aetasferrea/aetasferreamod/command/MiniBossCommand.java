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
 * @updated 08/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.command;

import java.util.List;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import com.aetasferrea.aetasferreamod.entity.boss.MiniBossManager;
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
import net.minecraftforge.fml.common.Mod;

// ---------- CLASS: MINI BOSS COMMAND
@Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MiniBossCommand {

    // ---------- MINI-BOSS TYPES
    private static final List<String> BOSS_TYPES = List.of(
        "vigil", "castellan", "knight_unenchanted", "knight", 
        "both_vigil_castellan", "knight_enchanted_and_double", "diamond_knight"
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
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(BOSS_TYPES, builder))
                                .executes(MiniBossCommand::spawnBoss)
                            )
                        )
                    )
            );
        }

    // ---------- COMMAND EXECUTION LOGIC
    @SuppressWarnings("null")
    private static int spawnBoss(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String type = StringArgumentType.getString(context, "type");
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        
        ServerPlayer player = null;
        if (source.getEntity() instanceof ServerPlayer sp) {
            player = sp;
        }

        // Delegate spelling and safety checks to the MiniBossManager
        boolean success = MiniBossManager.spawnBossAtPosition(level, pos, type, player);

        if (success) {
            source.sendSuccess(() -> Component.translatable("message.aetasferreamod.command.spawned_miniboss", type), true);
            return 1;
        } else {
            source.sendFailure(Component.translatable("message.aetasferreamod.command.failed_miniboss"));
            return 0;
        }
    }
}
