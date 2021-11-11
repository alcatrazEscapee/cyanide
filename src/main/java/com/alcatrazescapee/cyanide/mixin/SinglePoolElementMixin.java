/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin;

import java.util.function.Supplier;

import net.minecraft.world.level.levelgen.feature.structures.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import com.alcatrazescapee.cyanide.codec.MixinHooks;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SinglePoolElement.class)
public abstract class SinglePoolElementMixin
{
    @Inject(method = "processorsCodec", at = @At("HEAD"), cancellable = true)
    private static <E extends SinglePoolElement> void improvedProcessorsCodec(CallbackInfoReturnable<RecordCodecBuilder<E, Supplier<StructureProcessorList>>> cir)
    {
        cir.setReturnValue(MixinHooks.makeSinglePoolElementProcessorsCodec());
    }
}
