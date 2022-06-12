package com.alcatrazescapee.cyanide.mixin;

import java.util.Optional;
import java.util.function.Function;

import com.google.gson.JsonElement;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryLoader;
import net.minecraft.resources.RegistryResourceAccess;
import net.minecraft.resources.ResourceKey;

import com.alcatrazescapee.cyanide.codec.MixinHooks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RegistryLoader.class)
public abstract class RegistryLoaderMixin
{
    /**
     * {@link DataResult#flatMap(Function)} will only append error messages if a partial result is present, NOT if an error is present.
     * And it also drops the partial result from the first error. So, we need to restore it in order to get sane error concatenation.
     * This fixes instances where only the first two errors would be reported.
     * - The first would be reported as per normal, but the result would be demoted to a partial result
     * - The second would take the partial result, append errors, but then use the partial result from the second (which returns none). So at this point there's no partial result, but two errors.
     * - Any further errors would be absorbed without partial results.
     */
    @Redirect(method = "overrideRegistryFromResources", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/DataResult;flatMap(Ljava/util/function/Function;)Lcom/mojang/serialization/DataResult;", remap = false))
    private <E> DataResult<WritableRegistry<E>> flatMapSettingPartialResult(DataResult<WritableRegistry<E>> result, Function<? super WritableRegistry<E>, ? extends DataResult<WritableRegistry<E>>> overrideElementFromResources, WritableRegistry<E> registry)
    {
        return result.flatMap(overrideElementFromResources).setPartial(registry);
    }

    @Inject(method = "overrideElementFromResources(Lnet/minecraft/core/WritableRegistry;Lnet/minecraft/resources/ResourceKey;Lcom/mojang/serialization/Codec;Lnet/minecraft/resources/ResourceKey;Ljava/util/Optional;Lcom/mojang/serialization/DynamicOps;)Lcom/mojang/serialization/DataResult;", at = @At("RETURN"), cancellable = true)
    private <E> void appendFileToReadAndRegisterElement(WritableRegistry<E> registry, ResourceKey<? extends Registry<E>> registryKey, Codec<E> elementCodec, ResourceKey<E> elementKey, Optional<RegistryResourceAccess.EntryThunk<E>> thunk, DynamicOps<JsonElement> ops, CallbackInfoReturnable<DataResult<Holder<E>>> cir)
    {
        cir.setReturnValue(MixinHooks.appendRegistryFileError(cir.getReturnValue(), ops, registryKey, elementKey.location()));
    }
}
