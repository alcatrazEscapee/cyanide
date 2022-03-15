/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin;

import java.util.Map;

import com.google.gson.JsonElement;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryLoader;
import net.minecraft.resources.ResourceKey;

import com.alcatrazescapee.cyanide.codec.MixinHooks;
import com.mojang.serialization.DynamicOps;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RegistryAccess.class)
public interface RegistryAccessMixin
{
//    @Inject(method = "builtinCopy", at = @At("RETURN"))
//    private static void captureCurrentRegistryAccess(CallbackInfoReturnable<RegistryAccess.Writable> cir)
//    {
//        MixinHooks.captureRegistryAccess(cir.getReturnValue());
//    }
//
//    @Inject(method = "load", at = @At("HEAD"), cancellable = true)
//    private static void loadCollectingErrors(RegistryAccess.Writable registryAccess, DynamicOps<JsonElement> ops, RegistryLoader loader, CallbackInfo ci)
//    {
//        MixinHooks.readRegistries(registryAccess, ops, RegistryAccess.REGISTRIES, loader);
//        ci.cancel();
//    }
}
