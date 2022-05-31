/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.util.*;
import java.util.function.Consumer;

import org.apache.commons.lang3.mutable.MutableInt;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.CountConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import com.mojang.serialization.Codec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FeatureCycleTest extends TestHelper
{
    final boolean useNonEqualFeatures = true;

    final Map<Object, String> names = new IdentityHashMap<>();
    final MutableInt idCounter = new MutableInt(0);

    @BeforeEach
    public void clear()
    {
        names.clear();
        idCounter.setValue(0);
    }

    @Test
    public void testCycleDuplicateAdjacentFeature()
    {
        final Holder<PlacedFeature> feature = feature("duplicate");

        expectCycle(
            """
            A feature cycle was found.
                        
            Cycle:
            At step 0
            Feature 'duplicate'
              must be before 'duplicate' (defined in 'biome' at index 0, 1)
            """,
            biome("biome", b -> b
                .addFeature(0, feature)
                .addFeature(0, feature)
            )
        );
    }

    @Test
    public void testCycleDuplicateNonAdjacentFeature()
    {
        final Holder<PlacedFeature> feature = feature("duplicate");
        final Holder<PlacedFeature> innocent = feature("innocent");

        expectCycle(
            """
            A feature cycle was found.
                            
            Cycle:
            At step 0
            Feature 'duplicate'
              must be before 'innocent' (defined in 'biome' at index 0, 1)
              must be before 'duplicate' (defined in 'biome' at index 1, 2)
            """,
            biome("biome", b -> b
                .addFeature(0, feature)
                .addFeature(0, innocent)
                .addFeature(0, feature)
            )
        );
    }

    @Test
    public void testCycleMultipleBiomes()
    {
        final Holder<PlacedFeature> dummy1 = feature("dummy1");
        final Holder<PlacedFeature> dummy2 = feature("dummy1");
        final Holder<PlacedFeature> target1 = feature("target1");
        final Holder<PlacedFeature> target2 = feature("target2");
        final Holder<PlacedFeature> diversion = feature("diversion");

        expectCycle(
            """
                A feature cycle was found.
                            
                Cycle:
                At step 0
                Feature 'target1'
                  must be before 'dummy1' (defined in 'biome1_2' at index 0, 1)
                  must be before 'target2' (defined in 'biome1_2' at index 1, 2)
                  must be before 'dummy1' (defined in 'biome2_1' at index 0, 1)
                  must be before 'target1' (defined in 'biome2_1' at index 1, 2)
                """,
            biome("biome1_2", b -> b
                .addFeature(0, target1)
                .addFeature(0, dummy1)
                .addFeature(0, target2)
            ),
            biome("biome2_1", b -> b
                .addFeature(0, target2)
                .addFeature(0, dummy2)
                .addFeature(0, target1)
            ),
            biome("biome3", b -> b
                .addFeature(0, dummy2)
                .addFeature(0, diversion)
                .addFeature(0, dummy1)
            )
        );
    }

    @Test
    public void testCycleEqualFeatureData()
    {
        final Holder<PlacedFeature> firstIndex1 = feature("firstIndex1");
        final Holder<PlacedFeature> firstIndex2 = feature("firstIndex2");
        final Holder<PlacedFeature> secondIndex1 = feature("secondIndex1");
        final Holder<PlacedFeature> secondIndex2 = feature("secondIndex2");
        final Holder<PlacedFeature> dummy1 = feature("dummy1");
        final Holder<PlacedFeature> dummy2 = feature("dummy2");

        expectCycle(
            """
                A feature cycle was found.
                                
                Cycle:
                At step 0
                Feature 'firstIndex2'
                  must be before 'secondIndex1' (defined in 'biome_1' at index 2, 3)
                  must be before 'secondIndex2' (defined in 'biome_4' at index 0, 1)
                  must be before 'firstIndex1' (defined in 'biome_2' at index 2, 3)
                  must be before 'firstIndex2' (defined in 'biome_3' at index 0, 1)
                """,
            biome("biome_1", b -> b
                .addFeature(0, dummy1)
                .addFeature(0, dummy2)
                .addFeature(0, firstIndex2)
                .addFeature(0, secondIndex1)
            ),
            biome("biome_2", b -> b
                .addFeature(0, dummy1)
                .addFeature(0, dummy2)
                .addFeature(0, secondIndex2)
                .addFeature(0, firstIndex1)
            ),
            biome("biome_3", b -> b
                .addFeature(0, firstIndex1)
                .addFeature(0, firstIndex2)
            ),
            biome("biome_4", b -> b
                .addFeature(0, secondIndex1)
                .addFeature(0, secondIndex2)
            )
        );
    }

    @Test
    public void testNoCycleDuplicateButDifferentStep()
    {
        final Holder<PlacedFeature> feature = feature("innocent");

        expectNoCycle(
            biome("biome_same_feature_different_steps", b -> b
                .addFeature(0, feature)
                .addFeature(1, feature)
            )
        );
    }

    @Test
    public void testNoCycleEqualFeatureData()
    {
        final Holder<PlacedFeature> first = feature("first");
        final Holder<PlacedFeature> second = feature("second");
        final Holder<PlacedFeature> third = feature("third");
        final Holder<PlacedFeature> fourth = feature("fourth");

        expectNoCycle(
            biome("biome_1", b -> b
                .addFeature(0, first)
                .addFeature(0, second)
            ),
            biome("biome_2", b -> b
                .addFeature(0, second)
                .addFeature(0, third)
            )
        );
    }

    @SafeVarargs
    private void expectNoCycle(Holder<Biome>... biomes)
    {
        // Neither should throw
        final List<Holder<Biome>> list = Arrays.asList(biomes);
        biomeSourceBuildFeaturesPerStep(list);
        FeatureCycleDetector.buildFeaturesPerStep(list);
    }

    @SafeVarargs
    private void expectCycle(String expectedError, Holder<Biome>... biomes)
    {
        final List<Holder<Biome>> list = Arrays.asList(biomes);
        try
        {
            FeatureCycleDetector.buildFeaturesPerStep(list, o -> names.getOrDefault(o, "biome?"), o -> names.getOrDefault(o, "feature?"));
            fail("No feature cycle detected by FeatureCycleDetector");
        }
        catch (FeatureCycleDetector.FeatureCycleException e)
        {
            assertEquals(expectedError, e.getMessage());
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
        final ConfiguredFeature<?, ?> feature = useNonEqualFeatures ?
            new ConfiguredFeature<>(Feature.SEA_PICKLE, new CountConfiguration(idCounter.getAndIncrement())) :
            new ConfiguredFeature<>(Feature.NO_OP, NoneFeatureConfiguration.INSTANCE);
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
            .temperature(idCounter.getAndIncrement())
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
