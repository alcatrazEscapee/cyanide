/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin;

import org.apache.logging.log4j.Logger;
import net.minecraft.world.item.crafting.RecipeManager;

import com.alcatrazescapee.cyanide.Cyanide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin
{
    /**
     * Improve logging message, don't dump stacktrace
     */
    @Redirect(method = "apply", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false), require = 0)
    private void simplifyRecipeErrors(Logger logger, String message, Object p0, Object p1)
    {
        Cyanide.cleanRecipeError(message, p0, p1);
    }
}
