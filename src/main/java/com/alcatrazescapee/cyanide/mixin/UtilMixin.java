package com.alcatrazescapee.cyanide.mixin;

import net.minecraft.Util;

import com.alcatrazescapee.cyanide.Cyanide;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.types.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Util.class)
public abstract class UtilMixin
{
    /**
     * Prevent a data fixer lookup
     */
    @Inject(method = "fetchChoiceType", at = @At("HEAD"), cancellable = true)
    private static void noChoiceTypeForYou(DSL.TypeReference type, String choiceName, CallbackInfoReturnable<Type<?>> cir)
    {
        if (Cyanide.DANGER) cir.setReturnValue(null);
    }
}
