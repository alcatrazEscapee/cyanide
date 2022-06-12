package com.alcatrazescapee.cyanide.codec;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Unit;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

import com.alcatrazescapee.cyanide.mixin.accessor.*;
import com.alcatrazescapee.cyanide.platform.XPlatform;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class MixinHooks
{
    private static final GenerationStep.Decoration[] DECORATION_STEPS = GenerationStep.Decoration.values();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static @Nullable Codec<Holder<StructureProcessorList>> STRUCTURE_PROCESSOR_LIST_CODEC;

    public static List<FeatureSorter.StepFeatureData> buildFeaturesPerStepAndPopulateErrors(List<Holder<Biome>> allBiomes, Function<Holder<Biome>, List<HolderSet<PlacedFeature>>> biomeFeatures)
    {
        // This is delayed enough (as the underlying function is made lazy), so we can just retrieve the registry access from the server
        final MinecraftServer server = XPlatform.INSTANCE.getServer();
        if (server != null)
        {
            final RegistryAccess registryAccess = server.registryAccess();
            final Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registry.BIOME_REGISTRY);
            final Registry<PlacedFeature> placedFeatureRegistry = registryAccess.registryOrThrow(Registry.PLACED_FEATURE_REGISTRY);
            return FeatureCycleDetector.buildFeaturesPerStep(allBiomes, biomeFeatures, b -> idFor(biomeRegistry, b), f -> idFor(placedFeatureRegistry, f));
        }
        return FeatureCycleDetector.buildFeaturesPerStep(allBiomes, biomeFeatures);
    }

    public static WorldGenSettings printWorldGenSettingsError(DataResult<WorldGenSettings> result)
    {
        return result.getOrThrow(false, err -> LOGGER.error(
            "Error parsing worldgen settings after loading data packs\n" +
            "(This is usually an error due to invalid dimensions.)\n\n" +
            err.replaceAll("; ", "\n") +
            "\n"
        ));
    }

    public static void readRegistries(RegistryAccess.Writable writable, DynamicOps<JsonElement> ops, Map<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> registryData, RegistryLoader loader)
    {
        DataResult<Unit> root = DataResult.success(Unit.INSTANCE);

        final RegistryLoader.Bound bound = loader.bind(writable);
        for (RegistryAccess.RegistryData<?> data : registryData.values())
        {
            root = readRegistry(root, bound, ops, data);
        }
        if (root.error().isPresent())
        {
            throw new JsonParseException("Error(s) loading registries:" + root.error().get().message().replaceAll("\n; ", "\n") + "\n\n");
        }
    }

    public static <E> DataResult<Unit> readRegistry(DataResult<Unit> result, RegistryLoader.Bound bound, DynamicOps<JsonElement> ops, RegistryAccess.RegistryData<E> data)
    {
        return result.flatMap(u ->
                bound.overrideRegistryFromResources(data.key(), data.codec(), ops)
                    .map(e -> Unit.INSTANCE)
                    .mapError(e -> "\n\nError(s) loading registry " + data.key().location() + ":\n" + e.replaceAll("; ", "\n")))
            .setPartial(Unit.INSTANCE);
    }

    public static <E> DataResult<E> appendRegistryError(DataResult<E> result, ResourceKey<? extends Registry<?>> registry)
    {
        return result.mapError(e -> appendErrorLocation(e, "registry " + registry.location()));
    }

    public static <E> DataResult<E> appendRegistryReferenceError(DataResult<E> result, ResourceLocation id, ResourceKey<? extends Registry<?>> registry)
    {
        return result.mapError(e -> appendErrorLocation(e, "reference to \"" + id + "\" from " + registry.location()));
    }

    public static <E> DataResult<E> appendRegistryFileError(DataResult<E> result, DynamicOps<JsonElement> ops, ResourceKey<? extends Registry<?>> registry, ResourceLocation id)
    {
        result = result.mapError(e -> appendErrorLocation(e, "file \"" + registryFile(registry, id) + '"'));
        return appendRegistryEntrySourceError(result, ops, registry, id);
    }

    public static <E> DataResult<E> appendRegistryEntrySourceError(DataResult<E> result, DynamicOps<JsonElement> ops, ResourceKey<? extends Registry<?>> registryKey, ResourceLocation resourceLocation)
    {
        return result.mapError(error -> {
            if (ops instanceof RegistryOps<JsonElement> registryOps && registryOps.registryLoader().isPresent())
            {
                final RegistryLoader.Bound bound = registryOps.registryLoader().get();
                final RegistryResourceAccess resourceAccess = ((RegistryLoaderAccessor) bound.loader()).accessor$getResources();
                if (resourceAccess instanceof ResourceAccessWrapper wrapper)
                {
                    final @Nullable Resource resource = wrapper.manager().getResource(registryFileLocation(registryKey, resourceLocation)).orElse(null);
                    if (resource != null)
                    {
                        return appendErrorLocation(error, "data pack " + resource.sourcePackId());
                    }
                }
            }
            return error;
        });
    }

    public static Codec<Biome> makeBiomeCodec()
    {
        // Redirect many .optionalFieldOf calls to Codecs
        // Use custom codec for grass color modifier as an extensible enum
        final Codec<BiomeSpecialEffects> specialEffectsCodec = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("fog_color").forGetter(BiomeSpecialEffects::getFogColor),
            Codec.INT.fieldOf("water_color").forGetter(BiomeSpecialEffects::getWaterColor),
            Codec.INT.fieldOf("water_fog_color").forGetter(BiomeSpecialEffects::getWaterFogColor),
            Codec.INT.fieldOf("sky_color").forGetter(BiomeSpecialEffects::getSkyColor),
            Codecs.optionalFieldOf(Codec.INT, "foliage_color").forGetter(BiomeSpecialEffects::getFoliageColorOverride),
            Codecs.optionalFieldOf(Codec.INT, "grass_color").forGetter(BiomeSpecialEffects::getGrassColorOverride),
            Codecs.optionalFieldOf(Codecs.fromEnum("grass color modifier", BiomeSpecialEffects.GrassColorModifier::values), "grass_color_modifier", BiomeSpecialEffects.GrassColorModifier.NONE).forGetter(BiomeSpecialEffects::getGrassColorModifier),
            Codecs.optionalFieldOf(AmbientParticleSettings.CODEC, "particle").forGetter(BiomeSpecialEffects::getAmbientParticleSettings),
            Codecs.optionalFieldOf(SoundEvent.CODEC, "ambient_sound").forGetter(BiomeSpecialEffects::getAmbientLoopSoundEvent),
            Codecs.optionalFieldOf(AmbientMoodSettings.CODEC, "mood_sound").forGetter(BiomeSpecialEffects::getAmbientMoodSettings),
            Codecs.optionalFieldOf(AmbientAdditionsSettings.CODEC, "additions_sound").forGetter(BiomeSpecialEffects::getAmbientAdditionsSettings),
            Codecs.optionalFieldOf(Music.CODEC, "music").forGetter(BiomeSpecialEffects::getBackgroundMusic)
        ).apply(instance, BiomeSpecialEffectsAccessor::cyanide$new));

        // Add more Codecs.reporting() calls
        // Replace the Codec.list() with one that has indexes
        // Don't use the codec directly, instead use an improved registry entry codec implementation
        final Codec<PlacedFeature> placedFeatureCodec = RecordCodecBuilder.create(instance -> instance.group(
            Codecs.reporting(ConfiguredFeature.CODEC.fieldOf("feature"), "feature").forGetter(PlacedFeature::feature),
            Codecs.reporting(Codecs.list(PlacementModifier.CODEC).fieldOf("placement"), "placement").forGetter(PlacedFeature::placement)
        ).apply(instance, PlacedFeature::new));
        final Codec<HolderSet<PlacedFeature>> placedFeatureListCodec = Codecs.registryEntryListCodec(Registry.PLACED_FEATURE_REGISTRY, placedFeatureCodec);

        // Remove promotePartial calls, as logging at this level is pointless since we don't have a file or a registry name
        // Use much improved codecs for Codecs.list(), registry codecs
        // Improve the feature list with one that reports generation steps
        // Add additional calls to Codecs.reporting() to contextualize where things are going wrong.
        final MapCodec<BiomeGenerationSettings> biomeGenerationSettingsCodec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codecs.reporting(Codec.simpleMap(
                GenerationStep.Carving.CODEC,
                ConfiguredWorldCarver.LIST_CODEC,
                StringRepresentable.keys(GenerationStep.Carving.values())
            ).fieldOf("carvers"), "carvers").forGetter(c -> ((BiomeGenerationSettingsAccessor) c).cyanide$getCarvers()),
            Codecs.list(
                placedFeatureListCodec,
                (e, i) -> {
                    if (i >= 0 && i < DECORATION_STEPS.length)
                    {
                        return appendErrorLocation(e, "\"features\", step " + DECORATION_STEPS[i].name().toLowerCase(Locale.ROOT) + ", index " + i);
                    }
                    return appendErrorLocation(e, "\"features\", unknown step, index " + i);
                }
            ).fieldOf("features").forGetter(BiomeGenerationSettings::features)
        ).apply(instance, BiomeGenerationSettingsAccessor::cyanide$new));

        return XPlatform.INSTANCE.makeBiomeCodec(specialEffectsCodec, placedFeatureCodec, biomeGenerationSettingsCodec);
    }

    public static <E extends SinglePoolElement> RecordCodecBuilder<E, Holder<StructureProcessorList>> makeSinglePoolElementProcessorsCodec()
    {
        // Use improved structure processor list codec and add a reporting field to 'processors'
        return Codecs.reporting(structureProcessorListCodec().fieldOf("processors"), "processors").forGetter(e -> ((SinglePoolElementAccessor) e).cyanide$getProcessors());
    }

    public static RegistryResourceAccess wrapResourceAccess(ResourceManager manager)
    {
        return new ResourceAccessWrapper(RegistryResourceAccess.forResourceManager(manager), manager);
    }

    public static void cleanLootTableError(Logger logger, String message, Object p0, Object p1)
    {
        cleanError(logger, "Error parsing loot table {}.json : {}", message, p0, p1);
    }

    public static void cleanRecipeError(Logger logger, String message, Object p0, Object p1)
    {
        cleanError(logger, "Error parsing recipe {}.json : {}", message, p0, p1);
    }

    public static String appendErrorLocation(String error, String at)
    {
        return ensureNewLineSuffix(error) + "\tat: " + at;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object o)
    {
        return (T) o;
    }

    /**
     * @see StructureProcessorType
     */
    private static Codec<Holder<StructureProcessorList>> structureProcessorListCodec()
    {
        if (STRUCTURE_PROCESSOR_LIST_CODEC == null)
        {
            // Use improved list and either codecs, and use registry entry codec instead of the registry file codec
            // Add reporting codec for the processors field
            final Codec<StructureProcessorList> listObjectCodec = Codecs.list(StructureProcessorType.SINGLE_CODEC)
                .xmap(StructureProcessorList::new, StructureProcessorList::list);
            Codec<StructureProcessorList> directCodec = Codecs.either(
                ShapedCodec.likeMap(listObjectCodec.fieldOf("processors").codec()),
                ShapedCodec.likeList(listObjectCodec)
            ).xmap(e -> e.map(Function.identity(), Function.identity()), Either::left);
            STRUCTURE_PROCESSOR_LIST_CODEC = Codecs.registryEntryCodec(Registry.PROCESSOR_LIST_REGISTRY, directCodec);
        }
        return STRUCTURE_PROCESSOR_LIST_CODEC;
    }

    private static String registryFile(ResourceKey<? extends Registry<?>> registry, ResourceLocation resource)
    {
        final ResourceLocation file = registryFileLocation(registry, resource);
        return "data/" + file.getNamespace() + "/" + file.getPath();
    }

    /**
     * Mirrors the logic used in {@link net.minecraft.resources.RegistryResourceAccess#forResourceManager(ResourceManager)} for {@code parseElement()}.
     * Used to refer to a registry and element pair by its datapack defined file location.
     */
    private static ResourceLocation registryFileLocation(ResourceKey<? extends Registry<?>> registry, ResourceLocation resource)
    {
        return new ResourceLocation(resource.getNamespace(), registry.location().getPath() + "/" + resource.getPath() + ".json");
    }

    private static String ensureNewLineSuffix(String s)
    {
        return s.endsWith("\n") ? s : s + "\n";
    }

    private static void cleanError(Logger logger, String message, String fallbackMessage, Object possibleId, Object possibleError)
    {
        if (possibleId instanceof ResourceLocation id && possibleError instanceof Exception e)
        {
            logger.error(message, id, e.getMessage());
            return;
        }
        logger.error(fallbackMessage, possibleId, possibleError); // Fallback
    }

    private static <T> String idFor(Registry<T> registry, T element)
    {
        return registry.getResourceKey(element)
            .map(e -> e.location().toString())
            .orElseGet(() -> "[Unknown " + registry.key().location() + ']');
    }
}
