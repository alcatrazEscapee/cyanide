package com.alcatrazescapee.cyanide.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin
{
    @Redirect(
        method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false)
    )
    private void dontPrintStackTrace(Logger logger, String message, Object arg1, Object arg2)
    {
        final ResourceLocation id = (ResourceLocation) arg1;
        final Exception error = (Exception) arg2;
        logger.error("Parsing error loading recipe '{}': {}", id, error.getMessage());
    }
}
