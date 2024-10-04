package com.alcatrazescapee.cyanide.core;

import java.util.stream.Stream;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;

public final class MixinHooks
{
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object o)
    {
        return (T) o;
    }

    public static <T> MapCodec<T> fieldOf(MapCodec<T> codec, String field)
    {
        return new MapCodec<>() {
            @Override
            public <T1> Stream<T1> keys(DynamicOps<T1> ops)
            {
                return codec.keys(ops);
            }

            @Override
            public <T1> DataResult<T> decode(DynamicOps<T1> ops, MapLike<T1> input)
            {
                return codec.decode(ops, input).mapError(e -> e + "\n  at '" + field + "'");
            }

            @Override
            public <T1> RecordBuilder<T1> encode(T input, DynamicOps<T1> ops, RecordBuilder<T1> prefix)
            {
                return codec.encode(input, ops, prefix);
            }
        };
    }

    public static Codec<FloatProvider> validate(float min, float max, Codec<FloatProvider> codec)
    {
        return codec.validate(provider -> provider.getMinValue() < min
            ? DataResult.error(() -> "Value provider too low (must be >= %g), got %s".formatted(min, prettyPrint(provider)))
            : provider.getMaxValue() > max
                ? DataResult.error(() -> "Value provider too high (must be <= %g), got %s".formatted(max, prettyPrint(provider)))
                : DataResult.success(provider));
    }

    private static String prettyPrint(FloatProvider provider)
    {
        if (provider instanceof ConstantFloat c) return "" + c.getValue();
        final ResourceLocation id = BuiltInRegistries.FLOAT_PROVIDER_TYPE.getKey(provider.getType());
        return "%s[min=%g, max=%g]".formatted(id == null ? "" : id, provider.getMinValue(), provider.getMaxValue());
    }

    public static <T extends IntProvider> Codec<T> validate(int min, int max, Codec<T> codec)
    {
        return codec.validate(provider -> provider.getMinValue() < min
            ? DataResult.error(() -> "Value provider too low (must be >= %d), got %s".formatted(min, prettyPrint(provider)))
            : provider.getMaxValue() > max
                ? DataResult.error(() -> "Value provider too high (must be <= %d), got %s".formatted(max, prettyPrint(provider)))
                : DataResult.success(provider));
    }

    private static String prettyPrint(IntProvider provider)
    {
        // This is a pretty good heuristic, that's better than showing "[-1--1]" for a constant -1
        if (provider instanceof ConstantInt c) return "" + c.getValue();
        final ResourceLocation id = BuiltInRegistries.INT_PROVIDER_TYPE.getKey(provider.getType());
        return "%s[min=%d, max=%d]".formatted(id == null ? "" : id, provider.getMinValue(), provider.getMaxValue());
    }
}
