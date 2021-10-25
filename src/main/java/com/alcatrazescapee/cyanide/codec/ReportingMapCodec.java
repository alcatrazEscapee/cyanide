package com.alcatrazescapee.cyanide.codec;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.mojang.serialization.*;

public class ReportingMapCodec<E> extends DelegateMapCodec<E>
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
