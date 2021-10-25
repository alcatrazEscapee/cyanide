package com.alcatrazescapee.cyanide.codec;

import java.util.function.UnaryOperator;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public record ReportingCodec<E>(Codec<E> delegate, UnaryOperator<String> errorReporter) implements DelegateCodec<E>
{
    @Override
    public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> ops, T input)
    {
        return delegate().decode(ops, input).mapError(errorReporter);
    }
}
