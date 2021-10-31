/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.util.Collection;
import java.util.Optional;
import java.util.OptionalInt;

import com.google.gson.JsonElement;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;

public record ResourceAccessWrapper(RegistryReadOps.ResourceAccess delegate, ResourceManager manager) implements RegistryReadOps.ResourceAccess
{
    @Override
    public Collection<ResourceLocation> listResources(ResourceKey<? extends Registry<?>> registryKey)
    {
        return delegate.listResources(registryKey);
    }

    @Override
    public <E> Optional<DataResult<Pair<E, OptionalInt>>> parseElement(DynamicOps<JsonElement> ops, ResourceKey<? extends Registry<E>> registryKey, ResourceKey<E> resourceKey, Decoder<E> decoder)
    {
        return delegate.parseElement(ops, registryKey, resourceKey, decoder);
    }
}
