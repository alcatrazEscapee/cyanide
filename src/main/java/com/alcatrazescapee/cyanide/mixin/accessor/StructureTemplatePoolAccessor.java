/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin.accessor;

import java.util.List;

import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import com.mojang.datafixers.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructureTemplatePool.class)
public interface StructureTemplatePoolAccessor
{
    @Accessor("rawTemplates") List<Pair<StructurePoolElement, Integer>> cyanide$getRawTemplates();
}
