package com.aetasferrea.aetasferreamod.init;

import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import com.aetasferrea.aetasferreamod.world.structure.AddStructureSpawnsModifier;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.world.StructureModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class StructureModifierInit {
    public static final DeferredRegister<Codec<? extends StructureModifier>> STRUCTURE_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.STRUCTURE_MODIFIER_SERIALIZERS, AetasFerreaMod.MODID);

    public static final RegistryObject<Codec<AddStructureSpawnsModifier>> ADD_SPAWNS =
            STRUCTURE_MODIFIERS.register("add_spawns", () -> AddStructureSpawnsModifier.CODEC);
}
