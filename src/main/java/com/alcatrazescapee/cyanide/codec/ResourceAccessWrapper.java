/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.util.Map;
import java.util.Optional;

import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryResourceAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;

public record ResourceAccessWrapper(RegistryResourceAccess delegate, ResourceManager manager) implements RegistryResourceAccess
{
    @Override
    public <E> Map<ResourceKey<E>, EntryThunk<E>> listResources(ResourceKey<? extends Registry<E>> resourceKey)
    {
        return delegate.listResources(resourceKey);
    }

    @Override
    public <E> Optional<EntryThunk<E>> getResource(ResourceKey<E> resourceKey)
    {
        return delegate.getResource(resourceKey);
    }
}
