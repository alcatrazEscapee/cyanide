/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin;

import com.google.gson.JsonElement;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.ResourceManager;

import com.alcatrazescapee.cyanide.codec.MixinHooks;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RegistryOps.class)
public abstract class RegistryOpsMixin
{
    /**
     * Required to obtain the {@link ResourceManager} in {@link MixinHooks#appendRegistryEntrySourceError(DataResult, DynamicOps, ResourceKey, ResourceLocation)}
     */
    @Redirect(method = "createAndLoad(Lcom/mojang/serialization/DynamicOps;Lnet/minecraft/core/RegistryAccess$Writable;Lnet/minecraft/server/packs/resources/ResourceManager;)Lnet/minecraft/resources/RegistryOps;", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/RegistryResourceAccess;forResourceManager(Lnet/minecraft/server/packs/resources/ResourceManager;)Lnet/minecraft/resources/RegistryResourceAccess;"))
    private static RegistryResourceAccess wrapResourceManagerCreateAndLoad(ResourceManager manager)
    {
        return MixinHooks.wrapResourceAccess(manager);
    }

    /**
     * Cannot inject into {@link RegistryAccess#load(RegistryAccess.Writable, DynamicOps, RegistryLoader)} as it is an interface, so we inject here instead.
     * This is the top level registry load call, which we replace with our own, to deserialize registries independently and collect all errors until the end.
     */
    @Redirect(method = "createAndLoad(Lcom/mojang/serialization/DynamicOps;Lnet/minecraft/core/RegistryAccess$Writable;Lnet/minecraft/resources/RegistryResourceAccess;)Lnet/minecraft/resources/RegistryOps;", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/RegistryAccess;load(Lnet/minecraft/core/RegistryAccess$Writable;Lcom/mojang/serialization/DynamicOps;Lnet/minecraft/resources/RegistryLoader;)V"))
    private static void loadCollectingErrors(RegistryAccess.Writable registryAccess, DynamicOps<JsonElement> ops, RegistryLoader loader)
    {
        MixinHooks.readRegistries(registryAccess, ops, RegistryAccess.REGISTRIES, loader);
    }
}
