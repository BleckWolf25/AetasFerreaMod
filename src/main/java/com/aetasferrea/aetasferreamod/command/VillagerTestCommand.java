/**
 * @file VillagerTestCommand.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Brigadier command for spawning a structured circle of trade-test villagers and a wandering trader.
 *
 * @description
 * Registers a CommandDispatcher that adds the testtrading subcommand under the aetasferrea base literal,
 * verifies command operator permissions, and spawns 65 distinct villagers covering all professions and levels
 * alongside one wandering trader, setting all of their AI to disabled so they remain perfectly stationary.
 *
 * @since 23/06/2026
 * @updated 24/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.command;

// ---------- IMPORTS
import static java.util.Objects.requireNonNull;

import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// ---------- CLASS: VillagerTestCommand
@Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VillagerTestCommand {

    // ---------- COMMAND REGISTRATION
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("aetasferrea")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("testtrading")
                    .executes(VillagerTestCommand::spawnTradingCircle)
                )
        );
    }

    // ---------- COMMAND EXECUTION LOGIC
    private static int spawnTradingCircle(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Entity commander = source.getEntity();

        // Abort execution if the command sender is not a player
        if (!(commander instanceof ServerPlayer player)) {
            source.sendFailure(requireNonNull(Component.literal("Only players can execute this command.")));
            return 0;
        }

        ServerLevel level = source.getLevel();
        VillagerProfession[] professions = {
            VillagerProfession.ARMORER, VillagerProfession.BUTCHER, VillagerProfession.CARTOGRAPHER,
            VillagerProfession.CLERIC, VillagerProfession.FARMER, VillagerProfession.FISHERMAN,
            VillagerProfession.FLETCHER, VillagerProfession.LEATHERWORKER, VillagerProfession.LIBRARIAN,
            VillagerProfession.MASON, VillagerProfession.SHEPHERD, VillagerProfession.TOOLSMITH,
            VillagerProfession.WEAPONSMITH
        };

        double radius = 12.0;
        // Spawns 65 villagers (13 professions * 5 levels) plus 1 wandering trader
        int totalEntities = (professions.length * 5) + 1;
        int count = 0;

        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();

        // ---------- VILLAGER SPAWN (Spawn all professions levels 1 to 5)
        for (VillagerProfession profession : professions) {
            for (int lvl = 1; lvl <= 5; lvl++) {
                // Distribute entities evenly along the circular boundary
                double theta = (2.0 * Math.PI * count) / totalEntities;
                double x = playerX + radius * Math.cos(theta);
                double z = playerZ + radius * Math.sin(theta);
                // Orient entities inwards facing the center player coordinate
                float yaw = (float) (theta * 180.0 / Math.PI) - 90.0f;

                Villager villager = EntityType.VILLAGER.create(requireNonNull(level));
                if (villager != null) {
                    villager.moveTo(x, playerY, z, yaw, 0.0f);
                    // Disable mob AI entirely to simplify trade browsing
                    villager.setNoAi(true);
                    villager.setAge(0);
                    
                    VillagerData data = villager.getVillagerData().setProfession(requireNonNull(profession)).setLevel(lvl);
                    villager.setVillagerData(requireNonNull(data));
                    
                    level.addFreshEntity(villager);
                }
                count++;
            }
        }

        // ---------- WANDERING TRADER SPAWN (Spawn wandering trader at the end of the circle)
        double theta = (2.0 * Math.PI * count) / totalEntities;
        double x = playerX + radius * Math.cos(theta);
        double z = playerZ + radius * Math.sin(theta);
        float yaw = (float) (theta * 180.0 / Math.PI) - 90.0f;

        WanderingTrader trader = EntityType.WANDERING_TRADER.create(requireNonNull(level));
        if (trader != null) {
            trader.moveTo(x, playerY, z, yaw, 0.0f);
            // Lock wandering trader movement to keep them stationed
            trader.setNoAi(true);
            level.addFreshEntity(trader);
        }

        source.sendSuccess(() -> Component.literal("Successfully spawned trading circle with 65 villagers and 1 wandering trader!"), true);
        return 1;
    }
}
