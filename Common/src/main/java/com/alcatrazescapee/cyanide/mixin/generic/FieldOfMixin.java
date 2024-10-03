package com.alcatrazescapee.cyanide.mixin.generic;

import com.alcatrazescapee.cyanide.core.MixinHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.SimpleMapCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Wide-targeting mixin to particularly involved references to {@link Codec#fieldOf(String)}, which is a super-easy place to automatically
 * insert error reporting references to a given field.
 * <p>
 * Note we suppress errors because of how wide targeting this is, there are unresolved references that mixin can't prove it can find,
 * but everything may target something as needed, and require is set to zero for all injectors here.
 */
@Mixin({
    DimensionType.class,
    Biome.class,
    PlacedFeature.class,
    StructureSet.class,
    StructureTemplatePool.class,
    NoiseGeneratorSettings.class,
    BiomeGenerationSettings.class,
    MobSpawnSettings.class
})
@SuppressWarnings({
    "MixinAnnotationTarget",
    "InvalidInjectorMethodSignature"
})
public abstract class FieldOfMixin
{
    @WrapOperation(
        method = {
#if PLATFORM_FABRIC
            "method_28522(Lcom/mojang/serialization/codecs/RecordCodecBuilder$Instance;)Lcom/mojang/datafixers/kinds/App;", // DimensionType
            "method_30776(Lcom/mojang/serialization/codecs/RecordCodecBuilder$Instance;)Lcom/mojang/datafixers/kinds/App;", // Biome
            "method_39645(Lcom/mojang/serialization/codecs/RecordCodecBuilder$Instance;)Lcom/mojang/datafixers/kinds/App;", // PlacedFeature
            "method_41144(Lcom/mojang/serialization/codecs/RecordCodecBuilder$Instance;)Lcom/mojang/datafixers/kinds/App;", // StructureSet
            "method_28886(Lcom/mojang/serialization/codecs/RecordCodecBuilder$Instance;)Lcom/mojang/datafixers/kinds/App;", // StructureTemplatePool
            "method_28558(Lcom/mojang/serialization/codecs/RecordCodecBuilder$Instance;)Lcom/mojang/datafixers/kinds/App;", // NoiseGeneratorSettings
            "method_30802(Lcom/mojang/serialization/codecs/RecordCodecBuilder$Instance;)Lcom/mojang/datafixers/kinds/App;", // BiomeGenerationSettings
            "method_30791(Lcom/mojang/serialization/codecs/RecordCodecBuilder$Instance;)Lcom/mojang/datafixers/kinds/App;", // MobSpawnSettings
#else
            "lambda$static$0",
            "lambda$static$1",
            "lambda$static$2",
            "lambda$static$3",
            "lambda$static$4",
#endif
        },
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/serialization/Codec;fieldOf(Ljava/lang/String;)Lcom/mojang/serialization/MapCodec;",
            remap = false
        ),
        require = 0
    )
    private static <A> MapCodec<A> addErrorReportingToFieldOf(Codec<A> codec, String name, Operation<MapCodec<A>> original)
    {
        return MixinHooks.wrapFieldOf(original.call(codec, name), name);
    }

    @WrapOperation(
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
            target = "Lcom/mojang/serialization/codecs/SimpleMapCodec;fieldOf(Ljava/lang/String;)Lcom/mojang/serialization/MapCodec;",
            remap = false
        ),
        require = 0
    )
    private static <A> MapCodec<A> addErrorReportingToMapCodecFieldOf(SimpleMapCodec<?, ?> codec, String name, Operation<MapCodec<A>> original)
    {
        return MixinHooks.wrapFieldOf(original.call(codec, name), name);
    }
}
