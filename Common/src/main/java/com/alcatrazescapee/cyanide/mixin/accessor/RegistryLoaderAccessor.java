package com.alcatrazescapee.cyanide.mixin.accessor;

import net.minecraft.resources.RegistryLoader;
import net.minecraft.resources.RegistryResourceAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RegistryLoader.class)
public interface RegistryLoaderAccessor
{
    @Accessor("resources")
    RegistryResourceAccess accessor$getResources();
}
