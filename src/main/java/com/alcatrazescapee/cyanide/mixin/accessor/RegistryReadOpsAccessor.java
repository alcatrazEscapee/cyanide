/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin.accessor;

import java.util.function.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RegistryReadOps.class)
public interface RegistryReadOpsAccessor
{
    @Accessor("registryAccess")
    RegistryAccess cyanide$getRegistryAccess();

    @Accessor("resources")
    RegistryReadOps.ResourceAccess cyanide$getResources();

    @Invoker("readAndRegisterElement")
    <E> DataResult<Supplier<E>> cyanide$readAndRegisterElement(ResourceKey<? extends Registry<E>> registryKey, final WritableRegistry<E> registry, Codec<E> elementCodec, ResourceLocation id);
}
