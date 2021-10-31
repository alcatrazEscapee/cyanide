/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.util.stream.Stream;

import com.mojang.serialization.*;

public class DelegateMapCodec<A> extends MapCodec<A>
{
    private final MapCodec<A> delegate;

    protected DelegateMapCodec(MapCodec<A> delegate)
    {
        this.delegate = delegate;
    }

    public MapCodec<A> delegate()
    {
        return delegate;
    }

    @Override
    public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input)
    {
        return delegate().decode(ops, input);
    }

    @Override
    public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix)
    {
        return delegate().encode(input, ops, prefix);
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops)
    {
        return delegate().keys(ops);
    }
}
