package com.alcatrazescapee.cyanide.mixin;

import java.util.Map;

import com.google.gson.JsonParseException;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;

import com.alcatrazescapee.cyanide.codec.MixinHooks;
import com.mojang.serialization.DataResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RegistryAccess.class)
public abstract class RegistryAccessMixin
{
    @Shadow @Final static Map<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> REGISTRIES;

    @Inject(method = "load", at = @At("HEAD"), cancellable = true)
    private static void loadCollectingErrors(RegistryAccess registryAccess, RegistryReadOps<?> ops, CallbackInfo ci)
    {
        MixinHooks.readRegistries(registryAccess, ops, REGISTRIES);
        ci.cancel();
    }
}
