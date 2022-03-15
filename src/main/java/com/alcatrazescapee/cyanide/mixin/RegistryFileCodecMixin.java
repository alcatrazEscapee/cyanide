/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;

import com.alcatrazescapee.cyanide.codec.Codecs;
import com.mojang.serialization.Codec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RegistryFileCodec.class)
public abstract class RegistryFileCodecMixin
{
    /*@Inject(method = "homogeneousList", at = @At("HEAD"), cancellable = true)
    private static <E> void improvedListCodec(ResourceKey<? extends Registry<E>> registryKey, Codec<E> elementCodec, CallbackInfoReturnable<Codec<List<Supplier<E>>>> cir)
    {
        cir.setReturnValue(Codecs.registryEntryListCodec(registryKey, elementCodec));
    }*/
}
