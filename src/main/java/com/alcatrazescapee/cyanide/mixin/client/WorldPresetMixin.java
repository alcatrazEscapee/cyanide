package com.alcatrazescapee.cyanide.mixin.client;

import net.minecraft.client.gui.screens.worldselection.WorldPreset;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldPreset.class)
public abstract class WorldPresetMixin
{
    /**
     * Enable the debug world type as part of world creation.
     */
    @Inject(method = "isVisibleByDefault", at = @At("HEAD"), cancellable = true)
    private static void enableDebugWorldType(WorldPreset preset, CallbackInfoReturnable<Boolean> cir)
    {
        cir.setReturnValue(true);
    }
}
