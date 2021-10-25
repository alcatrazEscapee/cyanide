package com.alcatrazescapee.cyanide.mixin;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.surfacebuilders.ConfiguredSurfaceBuilder;

import com.alcatrazescapee.cyanide.codec.Codecs;
import com.alcatrazescapee.cyanide.mixin.accessor.BiomeGenerationSettingsAccessor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BiomeGenerationSettings.class)
public abstract class BiomeGenerationSettingsMixin
{
    @Shadow @Final @Mutable public static MapCodec<BiomeGenerationSettings> CODEC;

    static
    {
        // Remove promotePartial calls, as logging at this level is pointless since we don't have a file or a registry name
        // Replace ExtraCodecs calls with Codecs, that include names for what is null or invalid
        CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codecs.nonNullSupplier(ConfiguredSurfaceBuilder.CODEC.fieldOf("surface_builder"), "surface_builder").forGetter(BiomeGenerationSettings::getSurfaceBuilder),
            Codec.simpleMap(
                GenerationStep.Carving.CODEC,
                Codecs.nonNullSupplierList(ConfiguredWorldCarver.LIST_CODEC, "carver"),
                StringRepresentable.keys(GenerationStep.Carving.values())
            ).fieldOf("carvers").forGetter(c -> ((BiomeGenerationSettingsAccessor) c).cyanide$getCarvers()),
            Codecs.nonNullSupplierList(ConfiguredFeature.LIST_CODEC, "feature").listOf().fieldOf("features").forGetter(BiomeGenerationSettings::features),
            Codecs.nonNullSupplierList(ConfiguredStructureFeature.LIST_CODEC, "structure start").fieldOf("starts").forGetter(c -> (List<Supplier<ConfiguredStructureFeature<?, ?>>>) c.structures())
        ).apply(instance, BiomeGenerationSettingsAccessor::cyanide$new));
    }
}
