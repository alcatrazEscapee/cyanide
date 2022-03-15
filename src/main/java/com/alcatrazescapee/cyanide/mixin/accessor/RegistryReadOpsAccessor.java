/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin.accessor;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.RegistryResourceAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RegistryOps.class)
public interface RegistryReadOpsAccessor
{
    @Accessor("registryAccess")
    RegistryAccess cyanide$getRegistryAccess();
}
