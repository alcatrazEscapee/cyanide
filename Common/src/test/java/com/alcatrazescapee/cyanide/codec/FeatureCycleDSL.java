package com.alcatrazescapee.cyanide.codec;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
                final FeatureCycleDetector.FeatureCycleException err = assertThrows(FeatureCycleDetector.FeatureCycleException.class, () -> FeatureCycleDetector.buildFeaturesPerStep(biomes, b -> b.value().getGenerationSettings().features()));
                assertEquals(error, err.getMessage());
            });
        }

        TestBuilder expectVanillaCycle()
        {
            return run((biomes, idMap) -> assertThrows(IllegalStateException.class, () -> FeatureSorter.buildFeaturesPerStep(biomes, b -> b.value().getGenerationSettings().features(), true)));
        }

        TestBuilder expectNoCycle()
        {
            return run((biomes, idMap) -> assertDoesNotThrow(() -> FeatureCycleDetector.buildFeaturesPerStep(biomes, b -> b.value().getGenerationSettings().features())));
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
                    createBiome(biome.id, idCounter, builder ->
                        biome.features.forEach(feature ->
                            builder.addFeature(feature.step, featureMap.computeIfAbsent(feature.id, featureKey ->
                                createFeature(feature.id, idCounter, useIdentityEqualFeatures)))))))
                .toList();
            final Map<Object, String> idMap = new IdentityHashMap<>();
            biomeMap.forEach((id, biome) -> idMap.put(biome.value(), id));
            featureMap.forEach((id, feature) -> idMap.put(feature.value(), id));
            test.accept(biomes, idMap);
            return this;
        }

        private Holder<PlacedFeature> createFeature(String id, MutableInt idCounter, boolean useIdentityEqualFeatures)
        {
            final ConfiguredFeature<?, ?> feature = useIdentityEqualFeatures ?
                new ConfiguredFeature<>(Feature.NO_OP, NoneFeatureConfiguration.INSTANCE) :
                new ConfiguredFeature<>(Feature.SEA_PICKLE, new CountConfiguration(idCounter.getAndIncrement()));
            final PlacedFeature placed = new PlacedFeature(Holder.direct(feature), new ArrayList<>());
            return reference(id, placed, Registry.PLACED_FEATURE_REGISTRY);
        }

        private Holder<Biome> createBiome(String id, MutableInt idCounter, Consumer<BiomeGenerationSettings.Builder> features)
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
            return reference(id, biome, Registry.BIOME_REGISTRY);
        }
    }

    private static <T> Holder<T> reference(String id, T value, ResourceKey<? extends Registry<T>> registry)
    {
        @SuppressWarnings("ConstantConditions")
        final Holder.Reference<T> ref = Holder.Reference.createStandAlone(null, ResourceKey.create(registry, new ResourceLocation(id)));
        try
        {
            final Field field = Holder.Reference.class.getDeclaredField("value");
            field.setAccessible(true);
            field.set(ref, value);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
        return ref;
    }

    private static <T> T uncheck(Callable<T> action)
    {
        try { return action.call(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    record BiomeBuilder(String id, List<FeatureBuilder> features) {}
    record FeatureBuilder(int step, String id) {}
}
