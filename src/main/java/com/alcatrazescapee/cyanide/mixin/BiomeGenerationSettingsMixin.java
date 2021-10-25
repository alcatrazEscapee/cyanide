package com.alcatrazescapee.cyanide.mixin;

import net.minecraft.world.level.biome.BiomeGenerationSettings;

import com.alcatrazescapee.cyanide.codec.MixinHooks;
import com.mojang.serialization.MapCodec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BiomeGenerationSettings.class)
public abstract class BiomeGenerationSettingsMixin
{
    @Shadow @Final @Mutable public static MapCodec<BiomeGenerationSettings> CODEC;

    static
    {
        CODEC = MixinHooks.makeBiomeGenerationSettingsCodec();
    }
}
