package com.alcatrazescapee.cyanide.codec;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.gson.JsonParseException;
import org.apache.logging.log4j.Logger;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Unit;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.surfacebuilders.ConfiguredSurfaceBuilder;

import com.alcatrazescapee.cyanide.mixin.accessor.BiomeAccessor;
import com.alcatrazescapee.cyanide.mixin.accessor.BiomeGenerationSettingsAccessor;
import com.alcatrazescapee.cyanide.mixin.accessor.BiomeSpecialEffectsAccessor;
import com.alcatrazescapee.cyanide.mixin.accessor.RegistryReadOpsAccessor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import static net.minecraftforge.common.ForgeHooks.*;

public final class MixinHooks
{
    private static final GenerationStep.Decoration[] DECORATION_STEPS = GenerationStep.Decoration.values();

    public static void readRegistries(RegistryAccess registryAccess, RegistryReadOps<?> ops, Map<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> registryData)
    {
        DataResult<Unit> root = DataResult.success(Unit.INSTANCE);
        for (RegistryAccess.RegistryData<?> data : registryData.values())
        {
            root = readRegistry(root, registryAccess, ops, data);
        }
        if (root.error().isPresent())
        {
            throw new JsonParseException("Error(s) loading registries:" + root.error().get().message().replaceAll("\n; ", "\n") + "\n\n");
        }
    }

    public static <E> DataResult<Unit> readRegistry(DataResult<Unit> result, RegistryAccess registryAccess, RegistryReadOps<?> ops, RegistryAccess.RegistryData<E> data)
    {
        final ResourceKey<? extends Registry<E>> key = data.key();
        final MappedRegistry<E> registry = (MappedRegistry<E>) registryAccess.ownedRegistryOrThrow(key);
        return result.flatMap(u -> ops.decodeElements(registry, key, data.codec())
            .map(e -> Unit.INSTANCE)
            .mapError(e -> "\n\nError(s) loading registry " + key.location() + ":\n" + e.replaceAll("; ", "\n")));
    }

    public static <E> DataResult<E> appendRegistryError(DataResult<E> result, ResourceKey<? extends Registry<?>> registry)
    {
        return result.mapError(e -> appendErrorLocation(e, "registry " + registry.location()));
    }

    public static <E> DataResult<E> appendRegistryReferenceError(DataResult<E> result, ResourceLocation id, ResourceKey<? extends Registry<?>> registry)
    {
        return result.mapError(e -> appendErrorLocation(e, "reference to \"" + id + "\" from " + registry.location()));
    }

    public static <E> DataResult<E> appendRegistryFileError(DataResult<E> result, RegistryReadOps<?> ops, ResourceLocation id, ResourceKey<? extends Registry<?>> registry)
    {
        result = result.mapError(e -> appendErrorLocation(e, "file \"" + registryFile(registry, id) + '"'));
        return appendRegistryEntrySourceError(result, ops, registry, id);
    }

    public static <E> DataResult<E> appendRegistryEntrySourceError(DataResult<E> result, RegistryReadOps<?> ops, ResourceKey<? extends Registry<?>> registryKey, ResourceLocation resourceLocation)
    {
        if (((RegistryReadOpsAccessor) ops).cyanide$getResources() instanceof ResourceAccessWrapper wrapper)
        {
            try
            {
                final Resource resource = wrapper.manager().getResource(registryFileLocation(registryKey, resourceLocation));
                return result.mapError(e -> appendErrorLocation(e, "data pack " + resource.getSourceName()));
            }
            catch (IOException e) { /* Ignore */ }
        }
        return result;
    }

    public static Codec<Biome> makeBiomeCodec()
    {
        // Add Codecs.reporting() to key fields
        // Use improved codec for temperature settings and special effects codecs that use improved optionals and Codecs.reporting()

        final MapCodec<Biome.ClimateSettings> climateSettingsCodec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codecs.reporting(Biome.Precipitation.CODEC.fieldOf("precipitation"), "precipitation").forGetter(c -> c.precipitation),
            Codecs.reporting(Codec.FLOAT.fieldOf("temperature"), "temperature").forGetter(c -> c.temperature),
            Codecs.optionalFieldOf(Biome.TemperatureModifier.CODEC, "temperature_modifier", Biome.TemperatureModifier.NONE).forGetter(c -> c.temperatureModifier),
            Codecs.reporting(Codec.FLOAT.fieldOf("downfall"), "downfall").forGetter(c -> c.downfall)
        ).apply(instance, Biome.ClimateSettings::new));

        final Codec<BiomeSpecialEffects> specialEffectsCodec = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("fog_color").forGetter(BiomeSpecialEffects::getFogColor),
            Codec.INT.fieldOf("water_color").forGetter(BiomeSpecialEffects::getWaterColor),
            Codec.INT.fieldOf("water_fog_color").forGetter(BiomeSpecialEffects::getWaterFogColor),
            Codec.INT.fieldOf("sky_color").forGetter(BiomeSpecialEffects::getSkyColor),
            Codec.INT.optionalFieldOf("foliage_color").forGetter(BiomeSpecialEffects::getFoliageColorOverride),
            Codec.INT.optionalFieldOf("grass_color").forGetter(BiomeSpecialEffects::getGrassColorOverride),
            BiomeSpecialEffects.GrassColorModifier.CODEC.optionalFieldOf("grass_color_modifier", BiomeSpecialEffects.GrassColorModifier.NONE).forGetter(BiomeSpecialEffects::getGrassColorModifier),
            AmbientParticleSettings.CODEC.optionalFieldOf("particle").forGetter(BiomeSpecialEffects::getAmbientParticleSettings),
            SoundEvent.CODEC.optionalFieldOf("ambient_sound").forGetter(BiomeSpecialEffects::getAmbientLoopSoundEvent),
            AmbientMoodSettings.CODEC.optionalFieldOf("mood_sound").forGetter(BiomeSpecialEffects::getAmbientMoodSettings),
            AmbientAdditionsSettings.CODEC.optionalFieldOf("additions_sound").forGetter(BiomeSpecialEffects::getAmbientAdditionsSettings),
            Music.CODEC.optionalFieldOf("music").forGetter(BiomeSpecialEffects::getBackgroundMusic)
        ).apply(instance, BiomeSpecialEffectsAccessor::cyanide$new));

        return RecordCodecBuilder.create(instance -> instance.group(
            climateSettingsCodec.forGetter(b -> MixinHooks.<BiomeAccessor>cast(b).cyanide$getClimateSettings()),
            Biome.BiomeCategory.CODEC.fieldOf("category").forGetter(Biome::getBiomeCategory),
            Codecs.reporting(Codec.FLOAT.fieldOf("depth"), "depth").forGetter(Biome::getDepth),
            Codecs.reporting(Codec.FLOAT.fieldOf("scale"), "scale").forGetter(Biome::getScale),
            Codecs.reporting(specialEffectsCodec.fieldOf("effects"), "effects").forGetter(Biome::getSpecialEffects),
            BiomeGenerationSettings.CODEC.forGetter(Biome::getGenerationSettings),
            MobSpawnSettings.CODEC.forGetter(Biome::getMobSettings),
            ResourceLocation.CODEC.optionalFieldOf("forge:registry_name").forGetter(b -> Optional.ofNullable(b.getRegistryName()))
        ).apply(instance, (climate, category, depth, scale, effects, gen, spawns, name) -> enhanceBiome(name.orElse(null), climate, category, depth, scale, effects, gen, spawns, instance, BiomeAccessor::cyanide$new)));
    }

    public static MapCodec<BiomeGenerationSettings> makeBiomeGenerationSettingsCodec()
    {
        // Remove promotePartial calls, as logging at this level is pointless since we don't have a file or a registry name
        // Replace ExtraCodecs calls with Codecs, that include names for what is null or invalid
        // Improve the feature list with one that reports generation steps
        // Add additional calls to Codecs.reporting() to contextualize where things are going wrong.
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codecs.nonNullSupplier(ConfiguredSurfaceBuilder.CODEC.fieldOf("surface_builder"), "surface_builder").forGetter(BiomeGenerationSettings::getSurfaceBuilder),
            Codecs.reporting(Codec.simpleMap(
                GenerationStep.Carving.CODEC,
                Codecs.nonNullSupplierList(ConfiguredWorldCarver.LIST_CODEC, "carver"),
                StringRepresentable.keys(GenerationStep.Carving.values())
            ).fieldOf("carvers"), "carvers").forGetter(c -> ((BiomeGenerationSettingsAccessor) c).cyanide$getCarvers()),
            Codecs.list(
                Codecs.nonNullSupplierList(ConfiguredFeature.LIST_CODEC, "feature"),
                (e, i) -> {
                    if (i >= 0 && i < DECORATION_STEPS.length)
                    {
                        return appendErrorLocation(e, "\"features\", step " + DECORATION_STEPS[i].name().toLowerCase(Locale.ROOT) + ", index " + i);
                    }
                    return appendErrorLocation(e, "\"features\", unknown step, index " + i);
                }
            ).fieldOf("features").forGetter(BiomeGenerationSettings::features),
            Codecs.nonNullSupplierList(ConfiguredStructureFeature.LIST_CODEC, "structure start").fieldOf("starts").forGetter(c -> (List<Supplier<ConfiguredStructureFeature<?, ?>>>) c.structures())
        ).apply(instance, BiomeGenerationSettingsAccessor::cyanide$new));
    }

    public static RegistryReadOps.ResourceAccess wrapResourceAccess(ResourceManager manager)
    {
        return new ResourceAccessWrapper(RegistryReadOps.ResourceAccess.forResourceManager(manager), manager);
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

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object o)
    {
        return (T) o;
    }

    private static String registryFile(ResourceKey<? extends Registry<?>> registry, ResourceLocation resource)
    {
        final ResourceLocation file = registryFileLocation(registry, resource);
        return "data/" + file.getNamespace() + "/" + file.getPath();
    }

    /**
     * Mirrors the logic used in {@link net.minecraft.resources.RegistryReadOps.ResourceAccess#forResourceManager(ResourceManager)} for {@code parseElement()}.
     * Used to refer to a registry and element pair by it's datapack defined file location.
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
}
