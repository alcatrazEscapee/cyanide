/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.util.function.UnaryOperator;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;

public final class ReportingMapCodec<E> extends DelegateMapCodec<E>
{
    private final UnaryOperator<String> errorReporter;

    public ReportingMapCodec(MapCodec<E> delegate, UnaryOperator<String> errorReporter)
    {
        super(delegate);
        this.errorReporter = errorReporter;
    }

    @Override
    public <T> DataResult<E> decode(DynamicOps<T> ops, MapLike<T> input)
    {
        return super.decode(ops, input).mapError(errorReporter);
    }
}
