/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.util.Collection;
import java.util.Optional;

import com.google.gson.JsonElement;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryResourceAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;

public record ResourceAccessWrapper(RegistryResourceAccess delegate, ResourceManager manager) implements RegistryResourceAccess
{
    @Override
    public <E> Collection<ResourceKey<E>> listResources(ResourceKey<? extends Registry<E>> resourceKey)
    {
        return delegate.listResources(resourceKey);
    }

    @Override
    public <E> Optional<DataResult<ParsedEntry<E>>> parseElement(DynamicOps<JsonElement> ops, ResourceKey<? extends Registry<E>> registryKey, ResourceKey<E> resourceKey, Decoder<E> decoder)
    {
        return delegate.parseElement(ops, registryKey, resourceKey, decoder);
    }
}
