/**
 * @file EquineTestCommand.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Brigadier command for spawning custom equine entities for testing purposes.
 *
 * @description
 * Registers the `/aetasferrea spawnmount` subcommand tree on the Forge event bus, supporting horse (with
 * class selection), donkey, and mule variants each with optional tamed/untamed status arguments.
 * Entities are spawned at the executing player's position with appropriate stats and equipment.
 *
 * @since 20/05/2026
 * @updated 25/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.command;

// ---------- IMPORTS
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

import com.aetasferrea.aetasferreamod.entity.AetasDonkey;
import com.aetasferrea.aetasferreamod.entity.AetasMule;
import com.aetasferrea.aetasferreamod.entity.HorseEventHandler;
import com.aetasferrea.aetasferreamod.init.EntityInit;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// ---------- CLASS: EquineTestCommand

public class EquineTestCommand {

    // ---------- CONSTANTS
    private static final List<String> CLASSES = List.of("wild", "rouncey", "destrier", "courser", "palfrey");
    private static final List<String> TAMED_STATUSES = List.of("tamed", "untamed");

    // ---------- COMMAND REGISTRATION
    @SuppressWarnings("null")
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("aetasferrea")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("spawnmount")
                    .then(Commands.literal("horse")
                        .then(Commands.argument("class", StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(CLASSES, builder))
                            .then(Commands.argument("tamed", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(TAMED_STATUSES, builder))
                                .executes(EquineTestCommand::spawnHorseWithArgs)
                            )
                            .executes(context -> spawnHorse(context, StringArgumentType.getString(context, "class"), null))
                        )
                        // Default: spawn a rouncey when no class is specified
                        .executes(context -> spawnHorse(context, "rouncey", null))
                    )
                    .then(Commands.literal("donkey")
                        .then(Commands.argument("tamed", StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(TAMED_STATUSES, builder))
                            .executes(EquineTestCommand::spawnDonkeyWithArgs)
                        )
                        .executes(context -> spawnDonkey(context, null))
                    )
                    .then(Commands.literal("mule")
                        .then(Commands.argument("tamed", StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(TAMED_STATUSES, builder))
                            .executes(EquineTestCommand::spawnMuleWithArgs)
                        )
                        .executes(context -> spawnMule(context, null))
                    )
                )
        );
    }

    // ---------- ARGUMENT EXTRACTION HELPERS
    private static int spawnHorseWithArgs(CommandContext<CommandSourceStack> context) {
        String className = StringArgumentType.getString(context, "class");
        String tamedStatus = StringArgumentType.getString(context, "tamed");
        return spawnHorse(context, className, tamedStatus);
    }

    private static int spawnDonkeyWithArgs(CommandContext<CommandSourceStack> context) {
        String tamedStatus = StringArgumentType.getString(context, "tamed");
        return spawnDonkey(context, tamedStatus);
    }

    private static int spawnMuleWithArgs(CommandContext<CommandSourceStack> context) {
        String tamedStatus = StringArgumentType.getString(context, "tamed");
        return spawnMule(context, tamedStatus);
    }

    // ---------- SPAWN HORSE
    private static int spawnHorse(CommandContext<CommandSourceStack> context, String className, String tamedStatus) {
        CommandSourceStack source = context.getSource();
        @Nonnull ServerLevel level = Objects.requireNonNull(source.getLevel());

        Entity commander = source.getEntity();
        if (!(commander instanceof ServerPlayer player)) {
            source.sendFailure(Objects.requireNonNull(Component.translatable("message.aetasferreamod.command.only_players")));
            return 0;
        }

        int horseClass = switch (className.toLowerCase()) {
            case "wild"     -> HorseEventHandler.CLASS_WILD;
            case "rouncey"  -> HorseEventHandler.CLASS_ROUNCEY;
            case "destrier" -> HorseEventHandler.CLASS_DESTRIER;
            case "courser"  -> HorseEventHandler.CLASS_COURSER;
            case "palfrey"  -> HorseEventHandler.CLASS_PALFREY;
            default         -> -1;
        };

        if (horseClass == -1) {
            source.sendFailure(Objects.requireNonNull(Component.translatable("message.aetasferreamod.command.unknown_horse", className)));
            return 0;
        }

        boolean tamed;
        if (tamedStatus != null) {
            tamed = tamedStatus.equalsIgnoreCase("tamed");
        } else {
            // Wild horses default to untamed, all named classes default to tamed
            tamed = (horseClass != HorseEventHandler.CLASS_WILD);
        }

        HorseEventHandler horse = EntityInit.AETAS_HORSE.get().create(level);
        if (horse == null) {
            source.sendFailure(Objects.requireNonNull(Component.translatable("message.aetasferreamod.command.failed_horse")));
            return 0;
        }

        horse.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        horse.initializeWithClass(horseClass);

        final boolean isTamed = tamed;
        if (isTamed) {
            horse.setTamed(true);
            horse.setOwnerUUID(player.getUUID());
            horse.equipSaddleForTesting();
            // Palfrey and Rouncey classes support chest inventory
            if (horseClass == HorseEventHandler.CLASS_PALFREY || horseClass == HorseEventHandler.CLASS_ROUNCEY) {
                horse.setChestAndCreateInventory(true);
            }
        } else {
            horse.setTamed(false);
        }

        level.addFreshEntity(horse);
        source.sendSuccess(() -> Objects.requireNonNull(Component.translatable("message.aetasferreamod.command.spawned_horse", className, isTamed ? Component.translatable("message.aetasferreamod.command.tamed_equipped") : Component.translatable("message.aetasferreamod.command.untamed"))), true);
        return 1;
    }

    // ---------- SPAWN DONKEY
    private static int spawnDonkey(CommandContext<CommandSourceStack> context, String tamedStatus) {
        CommandSourceStack source = context.getSource();
        @Nonnull ServerLevel level = Objects.requireNonNull(source.getLevel());

        Entity commander = source.getEntity();
        if (!(commander instanceof ServerPlayer player)) {
            source.sendFailure(Objects.requireNonNull(Component.translatable("message.aetasferreamod.command.only_players")));
            return 0;
        }

        // Default to tamed when no status argument is provided
        boolean tamed = tamedStatus == null || tamedStatus.equalsIgnoreCase("tamed");

        AetasDonkey donkey = EntityInit.AETAS_DONKEY.get().create(level);
        if (donkey == null) {
            source.sendFailure(Objects.requireNonNull(Component.translatable("message.aetasferreamod.command.failed_donkey")));
            return 0;
        }

        donkey.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        donkey.rerollStats(Objects.requireNonNull(level.getRandom()));

        if (tamed) {
            donkey.setTamed(true);
            donkey.setOwnerUUID(player.getUUID());
            donkey.equipSaddleForTesting();
            donkey.setChestAndCreateInventory(true);
        } else {
            donkey.setTamed(false);
        }

        level.addFreshEntity(donkey);
        source.sendSuccess(() -> Objects.requireNonNull(Component.translatable("message.aetasferreamod.command.spawned_donkey", tamed ? Component.translatable("message.aetasferreamod.command.tamed_saddled_chested") : Component.translatable("message.aetasferreamod.command.untamed"))), true);
        return 1;
    }

    // ---------- SPAWN MULE
    private static int spawnMule(CommandContext<CommandSourceStack> context, String tamedStatus) {
        CommandSourceStack source = context.getSource();
        @Nonnull ServerLevel level = Objects.requireNonNull(source.getLevel());

        Entity commander = source.getEntity();
        if (!(commander instanceof ServerPlayer player)) {
            source.sendFailure(Objects.requireNonNull(Component.translatable("message.aetasferreamod.command.only_players")));
            return 0;
        }

        // Default to tamed when no status argument is provided
        boolean tamed = tamedStatus == null || tamedStatus.equalsIgnoreCase("tamed");

        AetasMule mule = EntityInit.AETAS_MULE.get().create(level);
        if (mule == null) {
            source.sendFailure(Objects.requireNonNull(Component.translatable("message.aetasferreamod.command.failed_mule")));
            return 0;
        }

        mule.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        mule.rerollStats(Objects.requireNonNull(level.getRandom()));

        if (tamed) {
            mule.setTamed(true);
            mule.setOwnerUUID(player.getUUID());
            mule.equipSaddleForTesting();
            mule.setChestAndCreateInventory(true);
        } else {
            mule.setTamed(false);
        }

        level.addFreshEntity(mule);
        source.sendSuccess(() -> Objects.requireNonNull(Component.translatable("message.aetasferreamod.command.spawned_mule", tamed ? Component.translatable("message.aetasferreamod.command.tamed_saddled_chested") : Component.translatable("message.aetasferreamod.command.untamed"))), true);
        return 1;
    }
}
