package com.alcatrazescapee.cyanide.mixin.accessor;

import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RegistryDataLoader.class)
public interface RegistryDataLoaderAccessor
{
    @Invoker("registryDirPath")
    static String invoke$registryDirPath(ResourceLocation id) { throw new AssertionError(); }
}
