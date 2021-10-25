package com.alcatrazescapee.cyanide.codec;

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;

import com.mojang.serialization.DataResult;

public final class MixinHooks
{
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
            .mapError(e -> "\n\nError(s) loading registry " + key.location() + ":\n" + e));
    }

    public static <E> DataResult<E> appendRegistryEntryError(DataResult<E> result, ResourceLocation id, ResourceKey<? extends Registry<?>> registry)
    {
        return result.mapError(e -> ensureNewLineSuffix(e) + "\tat: " + id + " from " + registry.location() + "\n");
    }

    public static <E> DataResult<E> appendRegistryJsonError(DataResult<E> result, Object possibleJson, ResourceKey<? extends Registry<?>> registry)
    {
        if (possibleJson instanceof JsonElement json)
        {
            return result.mapError(e -> ensureNewLineSuffix(e) + "\tat: registry " + registry.location() + "\n\tat: JSON " + json + "\n");
        }
        return result.mapError(e -> ensureNewLineSuffix(e) + "\tat: registry " + registry.location() + "\n");
    }

    private static String ensureNewLineSuffix(String s)
    {
        return s.endsWith("\n") ? s : s + "\n";
    }
}
