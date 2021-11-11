package com.alcatrazescapee.cyanide.mixin.accessor;

import java.util.function.Supplier;

import net.minecraft.world.level.levelgen.feature.structures.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SinglePoolElement.class)
public interface SinglePoolElementAccessor
{
    @Accessor("processors")
    Supplier<StructureProcessorList> cyanide$getProcessors();
}
