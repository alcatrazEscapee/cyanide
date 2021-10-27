package com.alcatrazescapee.cyanide.mixin;

import java.util.function.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;

import com.alcatrazescapee.cyanide.codec.Codecs;
import com.alcatrazescapee.cyanide.codec.MixinHooks;
import com.mojang.serialization.Codec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Biome.class)
public abstract class BiomeMixin
{
    @Shadow @Final @Mutable public static Codec<Biome> DIRECT_CODEC;
    @Shadow @Final @Mutable public static Codec<Supplier<Biome>> CODEC;

    static
    {
        DIRECT_CODEC = MixinHooks.makeBiomeCodec();
        CODEC = Codecs.registryEntryCodec(Registry.BIOME_REGISTRY, DIRECT_CODEC);
    }
}
