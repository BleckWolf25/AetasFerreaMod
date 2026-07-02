package com.aetasferrea.aetasferreamod.world.structure;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.common.world.ModifiableStructureInfo;
import net.minecraftforge.common.world.StructureModifier;

import java.util.List;

public record AddStructureSpawnsModifier(
        HolderSet<Structure> structures,
        List<MobSpawnSettings.SpawnerData> spawners
) implements StructureModifier {

    private static final Codec<List<MobSpawnSettings.SpawnerData>> SPAWNERS_CODEC = Codec.either(
            MobSpawnSettings.SpawnerData.CODEC.listOf(),
            MobSpawnSettings.SpawnerData.CODEC
    ).xmap(
            either -> either.map(list -> list, List::of),
            list -> list.size() == 1 ? Either.right(list.get(0)) : Either.left(list)
    );

    public static final Codec<AddStructureSpawnsModifier> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    RegistryCodecs.homogeneousList(Registries.STRUCTURE).fieldOf("structures").forGetter(AddStructureSpawnsModifier::structures),
                    SPAWNERS_CODEC.fieldOf("spawners").forGetter(AddStructureSpawnsModifier::spawners)
            ).apply(builder, AddStructureSpawnsModifier::new)
    );

    @Override
    public void modify(Holder<Structure> structure, Phase phase, ModifiableStructureInfo.StructureInfo.Builder builder) {
        if (phase == Phase.ADD && this.structures.contains(structure)) {
            for (MobSpawnSettings.SpawnerData spawner : this.spawners) {
                builder.getStructureSettings().getOrAddSpawnOverrides(MobCategory.MONSTER).addSpawn(spawner);
            }
        }
    }

    @Override
    public Codec<? extends StructureModifier> codec() {
        return CODEC;
    }
}
