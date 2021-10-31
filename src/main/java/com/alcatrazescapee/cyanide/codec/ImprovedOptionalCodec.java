/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.util.Optional;
import java.util.stream.Stream;

import com.mojang.serialization.*;

/**
 * Optional field that will error if the field is present, but invalid, rather than silently accepting.
 */
public final class ImprovedOptionalCodec<A> extends MapCodec<Optional<A>>
{
    private final String name;
    private final Codec<A> elementCodec;

    public ImprovedOptionalCodec(String name, Codec<A> elementCodec)
    {
        this.name = name;
        this.elementCodec = elementCodec;
    }

    @Override
    public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input)
    {
        final T value = input.get(name);
        if (value != null)
        {
            return elementCodec.parse(ops, value)
                .map(Optional::of)
                .mapError(e -> "Optional field \"" + name + "\" was invalid: " + e);
        }
        return DataResult.success(Optional.empty());
    }

    @Override
    public <T> RecordBuilder<T> encode(Optional<A> input, DynamicOps<T> ops, RecordBuilder<T> prefix)
    {
        if (input.isPresent())
        {
            return prefix.add(name, elementCodec.encodeStart(ops, input.get()));
        }
        return prefix;
    }

    @Override
    public <T> Stream<T> keys(final DynamicOps<T> ops)
    {
        return Stream.of(ops.createString(name));
    }
}
