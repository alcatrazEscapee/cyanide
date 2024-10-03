package com.alcatrazescapee.cyanide.mixin.generic;

import java.util.function.Consumer;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Wide-targeting mixin to remove references to {@link Codec#promotePartial(Consumer)}. This is a terrible
 * error handling mechanism, and it obfuscates actual errors while making it difficult for us to track down root causes.
 * <p>
 * As we are not able to modify DFU, effectively, this will just turn these calls into no-ops
 */
@Mixin({
    BiomeGenerationSettings.class,
    MobSpawnSettings.class
})
public abstract class PromotePartialMixin
{
    @Redirect(
        method = {
#if PLATFORM_FABRIC
            "method_30802(Lcom/mojang/serialization/codecs/RecordCodecBuilder$Instance;)Lcom/mojang/datafixers/kinds/App;", // BiomeGenerationSettings
            "method_30791(Lcom/mojang/serialization/codecs/RecordCodecBuilder$Instance;)Lcom/mojang/datafixers/kinds/App;", // MobSpawnSettings
#else
            "lambda$static$2",
            "lambda$static$3",
#endif
        },
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/serialization/Codec;promotePartial(Ljava/util/function/Consumer;)Lcom/mojang/serialization/Codec;",
            remap = false
        ),
        require = 0
    )
    private static <A> Codec<A> dontPromotePartialErrors(Codec<A> codec, Consumer<String> onError)
    {
        return codec;
    }
}
