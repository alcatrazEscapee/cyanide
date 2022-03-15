/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.mixin;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import com.alcatrazescapee.cyanide.codec.Codecs;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructureTemplatePool.class)
public abstract class StructureTemplatePoolMixin
{
    @Shadow @Final @Mutable public static Codec<StructureTemplatePool> DIRECT_CODEC;
    @Shadow @Final @Mutable public static Codec<Holder<StructureTemplatePool>> CODEC;

    static
    {
        CODEC = Codecs.registryEntryCodec(Registry.TEMPLATE_POOL_REGISTRY, DIRECT_CODEC);
    }

    @Dynamic("lambda method in <cinit>")
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;listOf()Lcom/mojang/serialization/Codec;", remap = false))
    private static Codec<List<Pair<StructurePoolElement, Integer>>> addReportingToCodec(Codec<Pair<StructurePoolElement, Integer>> codec)
    {
        return Codecs.list(codec);
    }
}
