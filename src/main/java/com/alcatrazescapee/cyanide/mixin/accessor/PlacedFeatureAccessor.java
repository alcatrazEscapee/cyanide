/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin.accessor;

import java.util.function.Supplier;

import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlacedFeature.class)
public interface PlacedFeatureAccessor
{
    @Accessor("feature")
    Supplier<ConfiguredFeature<?, ?>> cyanide$getFeature();
}
