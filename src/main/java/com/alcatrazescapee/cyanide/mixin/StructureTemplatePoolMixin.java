package com.alcatrazescapee.cyanide.mixin;

import java.util.function.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.feature.structures.StructureTemplatePool;

import com.alcatrazescapee.cyanide.codec.Codecs;
import com.alcatrazescapee.cyanide.codec.MixinHooks;
import com.mojang.serialization.Codec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(StructureTemplatePool.class)
public abstract class StructureTemplatePoolMixin
{
    @Shadow @Final @Mutable public static Codec<StructureTemplatePool> DIRECT_CODEC;
    @Shadow @Final @Mutable public static Codec<Supplier<StructureTemplatePool>> CODEC;

    static
    {
        DIRECT_CODEC = MixinHooks.makeStructureTemplatePoolCodec();
        CODEC = Codecs.registryEntryCodec(Registry.TEMPLATE_POOL_REGISTRY, DIRECT_CODEC);
    }
}
