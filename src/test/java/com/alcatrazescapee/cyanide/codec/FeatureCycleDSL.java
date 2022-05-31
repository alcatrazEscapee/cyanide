/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.lang3.mutable.MutableInt;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.CountConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public interface FeatureCycleDSL
{
    default TestBuilder build(BiomeBuilder... biomes)
    {
        return new TestBuilder(Arrays.asList(biomes), "", false);
    }

    default BiomeBuilder biome(String id, FeatureBuilder... features)
    {
        return new BiomeBuilder(id, Arrays.asList(features));
    }

    default FeatureBuilder feature(int step, String id)
    {
        return new FeatureBuilder(step, id);
    }

    record TestBuilder(List<BiomeBuilder> biomes, String error, boolean useIdentityEqualFeatures)
    {
        void expectCycleX4()
        {
            expectCycle()
                .expectVanillaCycle()
                .identityFeatures()
                .expectCycle()
                .expectVanillaCycle();
        }

        void expectNoCycleX4()
        {
            expectNoCycle()
                .expectNoCycleInVanilla()
                .identityFeatures()
                .expectNoCycle()
                .expectNoCycleInVanilla();
        }

        TestBuilder expectCycle()
        {
            return run((biomes, idMap) -> {
                final FeatureCycleDetector.FeatureCycleException err = assertThrows(FeatureCycleDetector.FeatureCycleException.class, () -> FeatureCycleDetector.buildFeaturesPerStep(biomes, b -> b.value().getGenerationSettings().features(), idMap::get, idMap::get));
                assertEquals(error, err.getMessage());
            });
        }

        TestBuilder expectVanillaCycle()
        {
            return run((biomes, idMap) -> assertThrows(IllegalStateException.class, () -> FeatureSorter.buildFeaturesPerStep(biomes, b -> b.value().getGenerationSettings().features(), true)));
        }

        TestBuilder expectNoCycle()
        {
            return run((biomes, idMap) -> assertDoesNotThrow(() -> FeatureCycleDetector.buildFeaturesPerStep(biomes, b -> b.value().getGenerationSettings().features(), idMap::get, idMap::get)));
        }

        TestBuilder expectNoCycleInVanilla()
        {
            return run((biomes, idMap) -> assertDoesNotThrow(() -> FeatureSorter.buildFeaturesPerStep(biomes, b -> b.value().getGenerationSettings().features(), true)));
        }

        TestBuilder error(String error)
        {
            return new TestBuilder(biomes, error, useIdentityEqualFeatures);
        }

        TestBuilder identityFeatures()
        {
            return new TestBuilder(biomes, error, true);
        }

        TestBuilder run(BiConsumer<List<Holder<Biome>>, Map<Object, String>> test)
        {
            final MutableInt idCounter = new MutableInt();
            final Map<String, Holder<Biome>> biomeMap = new HashMap<>();
            final Map<String, Holder<PlacedFeature>> featureMap = new HashMap<>();
            final List<Holder<Biome>> biomes = this.biomes.stream()
                .map(biome -> biomeMap.computeIfAbsent(biome.id, biomeKey ->
                    createBiome(idCounter, builder ->
                        biome.features.forEach(feature ->
                            builder.addFeature(feature.step, featureMap.computeIfAbsent(feature.id, featureKey ->
                                createFeature(idCounter, useIdentityEqualFeatures)))))))
                .toList();
            final Map<Object, String> idMap = new IdentityHashMap<>();
            biomeMap.forEach((id, biome) -> idMap.put(biome.value(), id));
            featureMap.forEach((id, feature) -> idMap.put(feature.value(), id));
            test.accept(biomes, idMap);
            return this;
        }

        private Holder<PlacedFeature> createFeature(MutableInt idCounter, boolean useIdentityEqualFeatures)
        {
            final ConfiguredFeature<?, ?> feature = useIdentityEqualFeatures ?
                new ConfiguredFeature<>(Feature.NO_OP, NoneFeatureConfiguration.INSTANCE) :
                new ConfiguredFeature<>(Feature.SEA_PICKLE, new CountConfiguration(idCounter.getAndIncrement()));
            final PlacedFeature placed = new PlacedFeature(Holder.direct(feature), new ArrayList<>());
            return Holder.direct(placed);
        }

        private Holder<Biome> createBiome(MutableInt idCounter, Consumer<BiomeGenerationSettings.Builder> features)
        {
            final BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder();
            features.accept(builder);
            final Biome biome = new Biome.BiomeBuilder()
                .precipitation(Biome.Precipitation.NONE)
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
            return Holder.direct(biome);
        }
    }

    record BiomeBuilder(String id, List<FeatureBuilder> features) {}
    record FeatureBuilder(int step, String id) {}
}
