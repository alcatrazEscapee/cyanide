/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin;

import net.minecraft.util.datafix.DataFixers;

import com.alcatrazescapee.cyanide.Cyanide;
import com.mojang.datafixers.DataFixerBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DataFixers.class)
public abstract class DataFixersMixin
{
    /**
     * Don't build data fixers
     */
    @Inject(method = "addFixers", at = @At("HEAD"), cancellable = true)
    private static void noDataFixingForYou(DataFixerBuilder builder, CallbackInfo ci)
    {
        if (Cyanide.DANGER) ci.cancel();
    }
}
