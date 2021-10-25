package com.alcatrazescapee.cyanide.codec;

import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;

import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.*;

/**
 * Improvement for list codecs which includes an optional index reporting function.
 */
public record ImprovedListCodec<A>(Codec<A> elementCodec, Reporter errorReporter) implements Codec<List<A>>
{
    @Override
    public <T> DataResult<T> encode(final List<A> input, final DynamicOps<T> ops, final T prefix)
    {
        final ListBuilder<T> builder = ops.listBuilder();
        for (final A a : input)
        {
            builder.add(elementCodec.encodeStart(ops, a));
        }
        return builder.build(prefix);
    }

    @Override
    public <T> DataResult<Pair<List<A>, T>> decode(final DynamicOps<T> ops, final T input)
    {
        return ops.getList(input).setLifecycle(Lifecycle.stable()).flatMap(stream -> {
            final ImmutableList.Builder<A> read = ImmutableList.builder();
            final Stream.Builder<T> failed = Stream.builder();
            final MutableObject<DataResult<Unit>> result = new MutableObject<>(DataResult.success(Unit.INSTANCE, Lifecycle.stable()));
            final MutableInt index = new MutableInt(0);

            stream.accept(t -> {
                final DataResult<Pair<A, T>> element = elementCodec.decode(ops, t)
                    .mapError(e -> errorReporter.apply(e, index.intValue()));
                element.error().ifPresent(e -> failed.add(t));
                result.setValue(result.getValue().apply2stable((r, v) -> {
                    read.add(v.getFirst());
                    return r;
                }, element));
                index.increment();
            });

            final ImmutableList<A> elements = read.build();
            final T errors = ops.createList(failed.build());
            final Pair<List<A>, T> pair = Pair.of(elements, errors);
            return result.getValue().map(unit -> pair).setPartial(pair);
        });
    }

    @FunctionalInterface
    public interface Reporter
    {
        String apply(String error, int index);
    }
}
