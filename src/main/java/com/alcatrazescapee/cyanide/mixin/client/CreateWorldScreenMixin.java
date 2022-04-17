/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin.client;

import java.util.function.Consumer;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

import com.alcatrazescapee.cyanide.codec.MixinHooks;
import com.mojang.serialization.DataResult;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin
{
    @Dynamic("Lambda method in private void tryApplyNewDataPacks(PackRepository)")
    @Redirect(method = "*(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/world/level/DataPackConfig;)Lcom/mojang/datafixers/util/Pair;", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/DataResult;getOrThrow(ZLjava/util/function/Consumer;)Ljava/lang/Object;", remap = false))
    private <E> E resultOrPartialWithImprovedErrorMessages(DataResult<E> result, boolean allowPartial, Consumer<String> onError)
    {
        return MixinHooks.cast(MixinHooks.printWorldGenSettingsError(MixinHooks.cast(result)));
    }
}
