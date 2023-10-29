package com.alcatrazescapee.cyanide.codec;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
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

    public static WorldGenSettings printWorldGenSettingsError(DataResult<WorldGenSettings> result)
    {
        return result.getOrThrow(false, err -> LOGGER.error(
            "Error parsing worldgen settings after loading data packs\n" +
            "(This is usually an error due to invalid dimensions.)\n\n" +
            err.replaceAll("; ", "\n") +
            "\n"
        ));
    }

    record RegistryDataPair<R>(RegistryDataLoader.RegistryData<R> data, MappedRegistry<R> registry)
    {
        static <R> RegistryDataPair<R> create(RegistryDataLoader.RegistryData<R> data)
        {
            return new RegistryDataPair<>(data, new MappedRegistry<>(data.key(), Lifecycle.stable()));
        }
    }

    /**
     * Replacement for {@link RegistryDataLoader#load(ResourceManager, RegistryAccess, List)}
     * While Mojang improved this logic in 1.19.3, they still create unnecessary stack traces, don't manage to catch all errors, and bury actual informative error messages.
     * This tosses pretty much everything, and rewrites the entire process.
     */
    public static RegistryAccess.Frozen loadAllRegistryData(ResourceManager resourceManager, RegistryAccess registryAccess, List<RegistryDataLoader.RegistryData<?>> registryData)
    {
        final List<? extends RegistryDataPair<?>> registryPairs = registryData.stream().map(RegistryDataPair::create).toList();
        final Map<ResourceKey<? extends Registry<?>>, RegistryOps.RegistryInfo<?>> registryInfo = new HashMap<>();

        registryAccess.registries().forEach(registryEntry -> registryInfo.put(registryEntry.key(), createImmutableRegistryInfo(registryEntry.value())));
        registryPairs.forEach(pair -> registryInfo.put(pair.registry().key(), createMutableRegistryInfo(pair.registry())));

        final RegistryOps.RegistryInfoLookup registryInfoLookup = new RegistryOps.RegistryInfoLookup() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> resourceKey)
            {
                return Optional.ofNullable((RegistryOps.RegistryInfo) registryInfo.get(resourceKey));
            }
        };

        final List<String> errors = new ArrayList<>();
        for (RegistryDataPair<?> pair : registryPairs)
        {
            loadRegistryData(resourceManager, registryInfoLookup, pair, errors);
        }

        boolean anyFreezeErrors = false;
        for (RegistryDataPair<?> pair : registryPairs)
        {
            try
            {
                pair.registry().freeze();
            }
            catch (IllegalStateException e)
            {
                if (!anyFreezeErrors)
                {
                    anyFreezeErrors = true;
                    errors.add("\n\nErrors occurred freezing registries.\nThese were elements that were referenced, but never defined (or their definition had an error above).\n\n");
                }

                final String unboundElementPrefix = "Unbound values in registry " + pair.registry.key() + ": [";
                if (e.getMessage().startsWith(unboundElementPrefix))
                {
                    errors.add("Missing references from the " + pair.registry.key().location().getPath() + " registry: [\n\t'" + e.getMessage().substring(unboundElementPrefix.length(), e.getMessage().length() - 1).replaceAll(", ", "',\n\t'") + "'\n]\n\n");
                }
                else
                {
                    errors.add(e.getMessage());
                }
            }
        }

        if (!errors.isEmpty())
        {
            throw new IllegalStateException("Error(s) loading registries:\n" + String.join("", errors));
        }

        return new RegistryAccess.ImmutableRegistryAccess(registryPairs.stream().map(RegistryDataPair::registry).toList()).freeze();
    }

    /**
     * From {@link RegistryDataLoader#loadRegistryContents(RegistryOps.RegistryInfoLookup, ResourceManager, ResourceKey, WritableRegistry, Decoder, Map)}
     */
    private static <R> void loadRegistryData(ResourceManager resourceManager, RegistryOps.RegistryInfoLookup registryInfoLookup, RegistryDataPair<R> pair, List<String> errors)
    {
        final ResourceKey<? extends Registry<R>> registryKey = pair.data.key();
        final String registryName = XPlatform.INSTANCE.registryDirPath(registryKey.location());
        final FileToIdConverter fileToIdConverter = FileToIdConverter.json(registryName);
        final RegistryOps<JsonElement> registryOps = RegistryOps.create(JsonOps.INSTANCE, registryInfoLookup);

        final List<String> errorsInRegistry = new ArrayList<>();

        for (Map.Entry<ResourceLocation, Resource> entry : fileToIdConverter.listMatchingResources(resourceManager).entrySet())
        {
            final ResourceLocation entryId = entry.getKey();
            final ResourceKey<R> entryKey = ResourceKey.create(registryKey, fileToIdConverter.fileToId(entryId));
            final Resource entryResource = entry.getValue();

            try
            {
                final Reader reader = entryResource.openAsReader();
                final JsonElement json = JsonParser.parseReader(reader);
                final DataResult<R> dataResult = pair.data().elementCodec().parse(registryOps, json);

                // Mirror the Forge patch in RegistryDataLoader
                if (!XPlatform.INSTANCE.shouldRegisterEntry(json)) continue;

                dataResult.result().ifPresent(result -> pair.registry().register(entryKey, result, entryResource.isBuiltin() ? Lifecycle.stable() : dataResult.lifecycle()));
                dataResult.error().ifPresent(error -> errorsInRegistry.add(
                    appendErrorLocation(appendErrorLocation(error.message(), registryName + " '" + entryId + "'"), "pack '" + entryResource.sourcePackId() + "'")));
            }
            catch (Exception e)
            {
                errorsInRegistry.add(appendErrorLocation(appendErrorLocation("External error occurred: " + e.getMessage(), registryName + " '" + entryId + "'"), "pack '" + entryResource.sourcePackId() + "'"));
            }
        }

        if (!errorsInRegistry.isEmpty())
        {
            final StringBuilder builder = new StringBuilder("\n\nErrors(s) loading registry ")
                .append(registryKey.location())
                .append(":\n\n");
            for (String error : errorsInRegistry)
            {
                builder.append(ensureNewLineSuffix(error.replaceAll("; ", "\n")));
            }
            errors.add(builder.toString());
        }
    }

    private static <T> RegistryOps.RegistryInfo<T> createMutableRegistryInfo(WritableRegistry<T> registry)
    {
        return new RegistryOps.RegistryInfo<>(registry.asLookup(), registry.createRegistrationLookup(), registry.registryLifecycle());
    }

    private static <T> RegistryOps.RegistryInfo<T> createImmutableRegistryInfo(Registry<T> registry)
    {
        return new RegistryOps.RegistryInfo<>(registry.asLookup(), registry.asTagAddingLookup(), registry.registryLifecycle());
    }

    public static <E> DataResult<E> appendRegistryError(DataResult<E> result, ResourceKey<? extends Registry<?>> registry)
    {
        return result.mapError(e -> appendErrorLocation(e, "registry " + registry.location()));
    }

    public static <E> DataResult<E> appendRegistryReferenceError(DataResult<E> result, ResourceLocation id, ResourceKey<? extends Registry<?>> registry)
    {
        return result.mapError(e -> appendErrorLocation(e, "reference to \"" + id + "\" from " + registry.location()));
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
        final Codec<HolderSet<PlacedFeature>> placedFeatureListCodec = Codecs.registryEntryListCodec(Registries.PLACED_FEATURE, placedFeatureCodec);

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
            STRUCTURE_PROCESSOR_LIST_CODEC = Codecs.registryEntryCodec(Registries.PROCESSOR_LIST, directCodec);
        }
        return STRUCTURE_PROCESSOR_LIST_CODEC;
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
}
