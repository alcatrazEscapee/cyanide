package com.alcatrazescapee.cyanide.core;

import java.util.stream.Stream;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import com.mojang.serialization.DataResult;

public final class MixinHooks
{
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object o)
    {
        return (T) o;
    }

    public static <T> MapCodec<T> wrapFieldOf(MapCodec<T> codec, String field)
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
}
