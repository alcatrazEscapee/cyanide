/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.Logger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import com.alcatrazescapee.cyanide.codec.MixinHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin
{
    @Shadow private Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes;

    /**
     * Improve logging message, don't dump stacktrace
     */
    @Redirect(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false))
    private void simplifyRecipeErrors(Logger logger, String message, Object p0, Object p1)
    {
        MixinHooks.cleanRecipeError(logger, message, p0, p1);
    }

    /**
     * Count recipes, not recipe types. Fix MC-190122
     */
    @Redirect(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At(value = "INVOKE", target = "Ljava/util/Map;size()I", remap = false))
    private int correctRecipeCount(Map<RecipeType<?>, ImmutableMap.Builder<ResourceLocation, Recipe<?>>> map)
    {
        return recipes.values().stream().mapToInt(Map::size).sum();
    }
}
