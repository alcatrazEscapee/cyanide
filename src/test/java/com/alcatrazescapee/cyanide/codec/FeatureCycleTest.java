/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.util.*;
import java.util.function.Consumer;

import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import com.mojang.serialization.Codec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class FeatureCycleTest
{
    final Map<Object, String> names = new HashMap<>();

    @BeforeAll
    public static void boostrap()
    {
        TestHelper.bootstrap();
    }

    @BeforeEach
    public void clear()
    {
        names.clear();
    }

    @Test
    public void testDuplicateAdjacentFeaturesCycle()
    {
        final PlacedFeature feature = feature("duplicate_feature");

        expectCycle(biome("biome_with_adjacent_features", b -> b
            .addFeature(0, () -> feature)
            .addFeature(0, () -> feature)));
    }

    @Test
    public void testDuplicateNonAdjacentFeaturesCycle()
    {
        final PlacedFeature feature = feature("duplicate");
        final PlacedFeature innocent = feature("innocent");

        expectCycle(biome("biome_with_duplicate_non_adjacent_features", b -> b
            .addFeature(0, () -> feature)
            .addFeature(0, () -> innocent)
            .addFeature(0, () -> feature)));
    }

    @Test
    public void testDuplicateInDifferentStepsNoCycle()
    {
        final PlacedFeature feature = feature("innocent");

        expectNoCycle(biome("biome_same_feature_different_steps", b -> b
            .addFeature(0, () -> feature)
            .addFeature(1, () -> feature)));
    }

    @Test
    public void testExampleWithCycle()
    {
        final PlacedFeature dummy1 = feature("dummy1");
        final PlacedFeature dummy2 = feature("dummy1");
        final PlacedFeature target1 = feature("target1");
        final PlacedFeature target2 = feature("target2");
        final PlacedFeature diversion = feature("diversion");

        expectCycle(
            biome("biome1_2", b -> b
                .addFeature(0, () -> target1)
                .addFeature(0, () -> dummy1)
                .addFeature(0, () -> target2)),
            biome("biome2_1", b -> b
                .addFeature(0, () -> target2)
                .addFeature(0, () -> dummy2)
                .addFeature(0, () -> target1)),
            biome("biome3", b -> b
                .addFeature(0, () -> dummy2)
                .addFeature(0, () -> diversion)
                .addFeature(0, () -> dummy1)));
    }

    private void expectNoCycle(Biome... biomes)
    {
        // Neither should throw
        final List<Biome> list = Arrays.asList(biomes);
        FeatureCycleDetector.buildFeaturesPerStep(list);
        biomeSourceBuildFeaturesPerStep(list);
    }

    private void expectCycle(Biome... biomes)
    {
        final List<Biome> list = Arrays.asList(biomes);
        try
        {
            FeatureCycleDetector.buildFeaturesPerStep(list, o -> names.getOrDefault(o, "biome?"), o -> names.getOrDefault(o, "feature?"));
            fail("No feature cycle detected by FeatureCycleDetector");
        }
        catch (FeatureCycleDetector.FeatureCycleException e)
        {
            System.out.println(e.getMessage());
        }

        assertThrows(IllegalStateException.class, () -> biomeSourceBuildFeaturesPerStep(list), "BiomeSource did not detect a feature cycle?");
    }

    private void biomeSourceBuildFeaturesPerStep(List<Biome> biomes)
    {
        new BiomeSource(biomes) {
            @Override
            protected Codec<? extends BiomeSource> codec()
            {
                return null;
            }

            @Override
            public BiomeSource withSeed(long seed)
            {
                return null;
            }

            @Override
            public Biome getNoiseBiome(int i, int j, int k, Climate.Sampler sampler)
            {
                return null;
            }
        };
    }

    private PlacedFeature feature(String name)
    {
        final ConfiguredFeature<?, ?> feature = Feature.NO_OP.configured(FeatureConfiguration.NONE);
        final PlacedFeature placed = new PlacedFeature(() -> feature, new ArrayList<>());
        names.put(placed, name);
        return placed;
    }

    private Biome biome(String name, Consumer<BiomeGenerationSettings.Builder> features)
    {
        final BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder();
        features.accept(builder);
        final Biome biome = new Biome.BiomeBuilder()
            .precipitation(Biome.Precipitation.NONE)
            .biomeCategory(Biome.BiomeCategory.NONE)
            .temperature(0)
            .downfall(0)
            .specialEffects(
                new BiomeSpecialEffects.Builder()
                    .waterColor(0)
                    .waterFogColor(0)
                    .fogColor(0)
                    .skyColor(0)
                    .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                    .build())
            .mobSpawnSettings(new MobSpawnSettings.Builder()
                .build())
            .generationSettings(builder.build())
            .build();
        names.put(biome, name);
        return biome;
    }
}
