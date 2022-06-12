package com.alcatrazescapee.cyanide.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public interface ShapedCodec<A> extends Codec<A>
{
    static <A> ShapedCodec<A> likeNumber(Codec<A> codec)
    {
        return new Impl<>(codec)
        {
            @Override
            public <T> DataResult<?> decodeShape(DynamicOps<T> ops, T input)
            {
                return ops.getNumberValue(input);
            }
        };
    }

    static <A> ShapedCodec<A> likeString(Codec<A> codec)
    {
        return new Impl<>(codec)
        {
            @Override
            public <T> DataResult<?> decodeShape(DynamicOps<T> ops, T input)
            {
                return ops.getStringValue(input);
            }
        };
    }

    static <A> ShapedCodec<A> likeMap(Codec<A> codec)
    {
        return new Impl<>(codec)
        {
            @Override
            public <T> DataResult<?> decodeShape(DynamicOps<T> ops, T input)
            {
                return ops.getMap(input);
            }
        };
    }

    static <A> ShapedCodec<A> likeList(Codec<A> codec)
    {
        return new Impl<>(codec)
        {
            @Override
            public <T> DataResult<?> decodeShape(DynamicOps<T> ops, T input)
            {
                return ops.getList(input);
            }
        };
    }

    static <A> ShapedCodec<A> likeAny(Codec<A> codec)
    {
        return new Impl<>(codec)
        {
            @Override
            public <T> DataResult<?> decodeShape(DynamicOps<T> ops, T input)
            {
                return DataResult.success(input);
            }
        };
    }

    /**
     * Decode the shape of the input.
     * If the input is of the expected input, return DataResult.success().
     * Otherwise, return a meta-error about the shape of the input.
     */
    <T> DataResult<?> decodeShape(DynamicOps<T> ops, T input);

    abstract class Impl<A> implements DelegateCodec<A>, ShapedCodec<A>
    {
        private final Codec<A> codec;

        Impl(Codec<A> codec)
        {
            this.codec = codec;
        }

        @Override
        public Codec<A> delegate()
        {
            return codec;
        }
    }
}
