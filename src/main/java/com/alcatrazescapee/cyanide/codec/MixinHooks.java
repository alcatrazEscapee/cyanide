package com.alcatrazescapee.cyanide.codec;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
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
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Unit;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.surfacebuilders.ConfiguredSurfaceBuilder;

import com.alcatrazescapee.cyanide.mixin.accessor.BiomeGenerationSettingsAccessor;
import com.alcatrazescapee.cyanide.mixin.accessor.RegistryReadOpsAccessor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class MixinHooks
{
    private static final GenerationStep.Decoration[] DECORATION_STEPS = GenerationStep.Decoration.values();

    public static void readRegistries(RegistryAccess registryAccess, RegistryReadOps<?> ops, Map<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> registryData)
    {
        DataResult<Unit> root = DataResult.success(Unit.INSTANCE);
        for(RegistryAccess.RegistryData<?> data : registryData.values())
        {
            root = MixinHooks.readRegistry(root, registryAccess, ops, data);
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

    public static <E> DataResult<E> appendRegistryEntryErrors(DataResult<E> result, RegistryReadOps<?> ops, ResourceLocation id, ResourceKey<? extends Registry<?>> registry)
    {
        result = result.mapError(e -> appendErrorLocation(e, id + " from " + registry.location()));
        return appendRegistryEntrySourceError(result, ops, registry, id);
    }

    public static <E> DataResult<E> appendRegistryJsonError(DataResult<E> result, Object possibleJson, ResourceKey<? extends Registry<?>> registry)
    {
        if (possibleJson instanceof JsonElement json)
        {
            return result.mapError(e -> appendErrorLocation(e, "registry " + registry.location() + "\n\tat: JSON " + json));
        }
        return result.mapError(e -> appendErrorLocation(e, "registry " + registry.location()));
    }

    public static <E> DataResult<E> appendRegistryEntrySourceError(DataResult<E> result, RegistryReadOps<?> ops, ResourceKey<? extends Registry<?>> registryKey, ResourceLocation resourceLocation)
    {
        if (((RegistryReadOpsAccessor) ops).cyanide$getResources() instanceof ResourceAccessWrapper wrapper)
        {
            // Mirrors RegistryReadOps.ResourceAccess
            final ResourceLocation resourceFileLocation = new ResourceLocation(resourceLocation.getNamespace(), registryKey.location().getPath() + "/" + resourceLocation.getPath() + ".json");
            try
            {
                final Resource resource = wrapper.manager().getResource(resourceFileLocation);
                return result.mapError(e -> appendErrorLocation(e, "data pack " + resource.getSourceName()));
            }
            catch (IOException e) { /* Ignore */ }
        }
        return result;
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
            ).fieldOf("carvers"), "\"carvers\"").forGetter(c -> ((BiomeGenerationSettingsAccessor) c).cyanide$getCarvers()),
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
