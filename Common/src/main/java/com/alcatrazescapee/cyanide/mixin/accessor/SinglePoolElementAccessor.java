package com.alcatrazescapee.cyanide.mixin.accessor;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SinglePoolElement.class)
public interface SinglePoolElementAccessor
{
    @Accessor("processors")
    Holder<StructureProcessorList> cyanide$getProcessors();
}
