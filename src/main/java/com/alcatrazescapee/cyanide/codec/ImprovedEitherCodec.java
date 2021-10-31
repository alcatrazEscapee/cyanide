/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public record ImprovedEitherCodec<F, S>(ShapedCodec<F> first, ShapedCodec<S> second) implements Codec<Either<F, S>>
{
    @Override
    public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> ops, T input)
    {
        final DataResult<?> firstShape = first.decodeShape(ops, input),
            secondShape = second.decodeShape(ops, input);
        if (firstShape.result().isPresent() && secondShape.error().isPresent())
        {
            // First shape
            return first.decode(ops, input).map(vo -> vo.mapFirst(Either::left));
        }
        else if (firstShape.error().isPresent() && secondShape.result().isPresent())
        {
            // Second shape
            return second.decode(ops, input).map(vo -> vo.mapFirst(Either::right));
        }
        else
        {
            // Unknown - either both, or neither, so we do a best guess and report both errors
            final DataResult<Pair<Either<F, S>, T>> firstRead = first.decode(ops, input).map(vo -> vo.mapFirst(Either::left));
            if (firstRead.result().isPresent())
            {
                return firstRead;
            }
            final DataResult<Pair<Either<F, S>, T>> secondRead = second.decode(ops, input).map(vo -> vo.mapFirst(Either::right));
            if (secondRead.result().isPresent())
            {
                return secondRead;
            }
            return firstRead.mapError(err -> secondRead.error().map(pr -> "Either [" + err + "; " + pr.message() + "]").orElse(err));
        }
    }

    @Override
    public <T> DataResult<T> encode(Either<F, S> input, DynamicOps<T> ops, T prefix)
    {
        return input.map(
            value1 -> first.encode(value1, ops, prefix),
            value2 -> second.encode(value2, ops, prefix)
        );
    }
}
