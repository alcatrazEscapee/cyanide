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
import net.minecraft.server.packs.resources.ResourceManager;

import com.alcatrazescapee.cyanide.codec.MixinHooks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RegistryReadOps.class)
public abstract class RegistryReadOpsMixin
{
    @Redirect(method = "createAndLoad(Lcom/mojang/serialization/DynamicOps;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/resources/RegistryReadOps;", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/RegistryReadOps$ResourceAccess;forResourceManager(Lnet/minecraft/server/packs/resources/ResourceManager;)Lnet/minecraft/resources/RegistryReadOps$ResourceAccess;"))
    private static RegistryReadOps.ResourceAccess wrapResourceManagerCreateAndLoad(ResourceManager manager)
    {
        return MixinHooks.wrapResourceAccess(manager);
    }

    @Redirect(method = "create(Lcom/mojang/serialization/DynamicOps;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/resources/RegistryReadOps;", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/RegistryReadOps$ResourceAccess;forResourceManager(Lnet/minecraft/server/packs/resources/ResourceManager;)Lnet/minecraft/resources/RegistryReadOps$ResourceAccess;"))
    private static RegistryReadOps.ResourceAccess wrapResourceManagerCreate(ResourceManager manager)
    {
        return MixinHooks.wrapResourceAccess(manager);
    }

    @Inject(method = "readAndRegisterElement", slice = @Slice(to = @At(value = "INVOKE", target = "Lcom/google/common/base/Suppliers;memoize(Lcom/google/common/base/Supplier;)Lcom/google/common/base/Supplier;")), at = @At(value = "RETURN", ordinal = 1), cancellable = true)
    private <E> void readAndRegisterElement(ResourceKey<? extends Registry<E>> registryKey, WritableRegistry<E> registry, Codec<E> elementCodec, ResourceLocation resourceId, CallbackInfoReturnable<DataResult<Supplier<E>>> cir)
    {
        cir.setReturnValue(MixinHooks.appendRegistryFileError(cir.getReturnValue(), MixinHooks.cast(this), resourceId, registryKey));
    }
}
