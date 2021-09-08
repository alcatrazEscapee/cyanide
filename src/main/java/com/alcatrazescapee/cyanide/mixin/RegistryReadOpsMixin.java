/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin;

import java.util.function.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import com.alcatrazescapee.cyanide.Cyanide;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RegistryReadOps.class)
public abstract class RegistryReadOpsMixin
{
    /**
     * Capture errors in world gen json and log them, with context
     */
    @Inject(method = "readAndRegisterElement", at = @At("RETURN"), require = 0)
    private <E> void readAndRegisterElement(ResourceKey<? extends Registry<E>> registryKey, WritableRegistry<E> registry, Codec<E> elementCodec, ResourceLocation resourceId, CallbackInfoReturnable<DataResult<Supplier<E>>> cir)
    {
        Cyanide.captureAndExpandError(registryKey, resourceId, cir.getReturnValue());
    }
}
