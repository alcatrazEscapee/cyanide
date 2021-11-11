/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import com.alcatrazescapee.cyanide.codec.MixinHooks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    /**
     * {@link DataResult#flatMap(Function)} will only append error messages if a partial result is present, NOT if an error is present.
     * And it also drops the partial result from the first error. So, we need to restore it in order to get sane error concatenation.
     * This fixes instances where only the first two errors would be reported.
     * - The first would be reported as per normal, but the result would be demoted to a partial result
     * - The second would take the partial result, append errors, but then use the partial result from the second (which returns none). So at this point there's no partial result, but two errors.
     * - Any further errors would be absorbed without partial results.
     */
    @Redirect(method = "decodeElements", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/DataResult;flatMap(Ljava/util/function/Function;)Lcom/mojang/serialization/DataResult;", remap = false))
    private <E> DataResult<MappedRegistry<E>> flatMapSettingPartialResult(DataResult<MappedRegistry<E>> dataResult, Function<? super MappedRegistry<E>, ? extends DataResult<MappedRegistry<E>>> readAndRegisterElement, MappedRegistry<E> registry)
    {
        return dataResult.flatMap(readAndRegisterElement).setPartial(registry);
    }

    /**
     * Vanilla does a very strange thing: If we try and parse an object from the {@code resources} and it returns an empty {@link java.util.Optional}, it then assumes that is a SUCCESS and will just happily return a data result that queries from the registry.
     * This is... broken, for many reasons. Most notably, because registry entries that return null later on are kind of horrible.
     */
    @Redirect(method = "readAndRegisterElement", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/DataResult;success(Ljava/lang/Object;Lcom/mojang/serialization/Lifecycle;)Lcom/mojang/serialization/DataResult;", remap = false), require = 1)
    private <E> DataResult<Supplier<E>> checkRegistryElementsThatFailToParse(E result, Lifecycle lifecycle, ResourceKey<? extends Registry<E>> registryKey, WritableRegistry<E> registry, Codec<E> elementCodec, ResourceLocation id)
    {
        return MixinHooks.registryElementDataResult(registryKey, registry, id);
    }

    @Inject(method = "readAndRegisterElement", at = @At(value = "RETURN"), cancellable = true)
    private <E> void appendFileToReadAndRegisterElement(ResourceKey<? extends Registry<E>> registryKey, WritableRegistry<E> registry, Codec<E> elementCodec, ResourceLocation resourceId, CallbackInfoReturnable<DataResult<Supplier<E>>> cir)
    {
        cir.setReturnValue(MixinHooks.appendRegistryFileError(cir.getReturnValue(), MixinHooks.cast(this), resourceId, registryKey));
    }
}
