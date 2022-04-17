/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.util.*;
import java.util.function.Consumer;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import com.mojang.serialization.Codec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class FeatureCycleTest extends TestHelper
{
    final Map<Object, String> names = new IdentityHashMap<>();

    @BeforeEach
    public void clear()
    {
        names.clear();
    }

    @Test
    public void testDuplicateAdjacentFeaturesCycle()
    {
        final Holder<PlacedFeature> feature = feature("duplicate_feature");

        expectCycle(biome("biome_with_adjacent_features", b -> b
            .addFeature(0, feature)
            .addFeature(0, feature)));
    }

    @Test
    public void testDuplicateNonAdjacentFeaturesCycle()
    {
        final Holder<PlacedFeature> feature = feature("duplicate");
        final Holder<PlacedFeature> innocent = feature("innocent");

        expectCycle(biome("biome_with_duplicate_non_adjacent_features", b -> b
            .addFeature(0, feature)
            .addFeature(0, innocent)
            .addFeature(0, feature)));
    }

    @Test
    public void testDuplicateInDifferentStepsNoCycle()
    {
        final Holder<PlacedFeature> feature = feature("innocent");

        expectNoCycle(biome("biome_same_feature_different_steps", b -> b
            .addFeature(0, feature)
            .addFeature(1, feature)));
    }

    @Test
    public void testExampleWithCycle()
    {
        final Holder<PlacedFeature> dummy1 = feature("dummy1");
        final Holder<PlacedFeature> dummy2 = feature("dummy1");
        final Holder<PlacedFeature> target1 = feature("target1");
        final Holder<PlacedFeature> target2 = feature("target2");
        final Holder<PlacedFeature> diversion = feature("diversion");

        expectCycle(
            biome("biome1_2", b -> b
                .addFeature(0, target1)
                .addFeature(0, dummy1)
                .addFeature(0, target2)),
            biome("biome2_1", b -> b
                .addFeature(0, target2)
                .addFeature(0, dummy2)
                .addFeature(0, target1)),
            biome("biome3", b -> b
                .addFeature(0, dummy2)
                .addFeature(0, diversion)
                .addFeature(0, dummy1)));
    }

    @SafeVarargs
    private void expectNoCycle(Holder<Biome>... biomes)
    {
        // Neither should throw
        final List<Holder<Biome>> list = Arrays.asList(biomes);
        FeatureCycleDetector.buildFeaturesPerStep(list);
        biomeSourceBuildFeaturesPerStep(list);
    }

    @SafeVarargs
    private void expectCycle(Holder<Biome>... biomes)
    {
        final List<Holder<Biome>> list = Arrays.asList(biomes);
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

    @SuppressWarnings("ConstantConditions")
    private void biomeSourceBuildFeaturesPerStep(List<Holder<Biome>> biomes)
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
            public Holder<Biome> getNoiseBiome(int i, int j, int k, Climate.Sampler sampler)
            {
                return null;
            }
        }.featuresPerStep();
    }

    private Holder<PlacedFeature> feature(String name)
    {
        // nextId is used to make all the features unique, as equality now is done automatically via records
        final ConfiguredFeature<?, ?> feature = new ConfiguredFeature<>(Feature.NO_OP, NoneFeatureConfiguration.INSTANCE);
        final PlacedFeature placed = new PlacedFeature(Holder.direct(feature), new ArrayList<>());
        names.put(placed, name);
        return Holder.direct(placed);
    }

    private Holder<Biome> biome(String name, Consumer<BiomeGenerationSettings.Builder> features)
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
        return Holder.direct(biome);
    }
}
