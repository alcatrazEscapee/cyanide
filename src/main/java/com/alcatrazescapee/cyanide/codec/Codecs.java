/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

import net.minecraftforge.common.util.Lazy;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;

public final class Codecs
{
    /**
     * Improvements for {@link Codec#either(Codec, Codec)} that use shaped codecs where possible and have improved error retention.
     */
    public static <F, S> Codec<Either<F, S>> either(Codec<F> first, Codec<S> second)
    {
        return new ImprovedEitherCodec<>(ShapedCodec.likeAny(first), ShapedCodec.likeAny(second));
    }

    public static <F, S> Codec<Either<F, S>> either(ShapedCodec<F> first, Codec<S> second)
    {
        return new ImprovedEitherCodec<>(first, ShapedCodec.likeAny(second));
    }

    public static <F, S> Codec<Either<F, S>> either(Codec<F> first, ShapedCodec<S> second)
    {
        return new ImprovedEitherCodec<>(ShapedCodec.likeAny(first), second);
    }

    public static <F, S> Codec<Either<F, S>> either(ShapedCodec<F> first, ShapedCodec<S> second)
    {
        return new ImprovedEitherCodec<>(first, second);
    }

    public static <E> Codec<List<E>> list(Codec<E> elementCodec)
    {
        return new ImprovedListCodec<>(elementCodec, (e, i) -> "at index " + i);
    }

    public static <E> Codec<List<E>> list(Codec<E> elementCodec, ImprovedListCodec.Reporter indexReporter)
    {
        return new ImprovedListCodec<>(elementCodec, indexReporter);
    }

    public static <E> Codec<E> reporting(Codec<E> codec, String at)
    {
        return new ReportingCodec<>(codec, e -> MixinHooks.appendErrorLocation(e, '"' + at + '"'));
    }

    public static <E> Codec<E> reporting(Codec<E> codec, UnaryOperator<String> errorReporter)
    {
        return new ReportingCodec<>(codec, errorReporter);
    }

    public static <E> MapCodec<E> reporting(MapCodec<E> codec, String at)
    {
        return new ReportingMapCodec<>(codec, e -> MixinHooks.appendErrorLocation(e, '"' + at + '"'));
    }

    public static <E> MapCodec<E> reporting(MapCodec<E> codec, UnaryOperator<String> errorReporter)
    {
        return new ReportingMapCodec<>(codec, errorReporter);
    }

    public static <E> MapCodec<E> optionalFieldOf(Codec<E> codec, String name, E defaultValue) {
        return optionalFieldOf(codec, name).xmap(
            o -> o.orElse(defaultValue),
            a -> Objects.equals(a, defaultValue) ? Optional.empty() : Optional.of(a)
        );
    }

    public static <F> MapCodec<Optional<F>> optionalFieldOf(Codec<F> elementCodec, String name)
    {
        return new ImprovedOptionalCodec<>(name, elementCodec);
    }

    /**
     * Like {@link StringRepresentable#fromEnum(Supplier, Function)} but with named errors.
     */
    public static <E extends Enum<E> & StringRepresentable> Codec<E> fromEnum(String id, Supplier<E[]> enumValues, Function<? super String, ? extends E> enumName)
    {
        final E[] values = enumValues.get();
        final String expectedIdRange = "[0, " + values.length + ")";
        final String expectedValues = "[" + Arrays.stream(values).map(StringRepresentable::getSerializedName).collect(Collectors.joining(", ")) + "]";
        return fromStringResolver(id, () -> expectedIdRange, () -> expectedValues, Enum::ordinal, i -> values[i], enumName);
    }

    /**
     * A variant of {@link #fromEnum(String, Supplier, Function)} for {@link net.minecraftforge.common.IExtensibleEnum}s
     */
    public static <E extends Enum<E> & StringRepresentable> Codec<E> fromExtensibleEnum(String id, Supplier<E[]> enumValues, Function<? super String, ? extends E> enumName)
    {
        final Lazy<E[]> values = Lazy.concurrentOf(enumValues);
        final String expectedIdRange = "[0, " + values.get().length + ")";
        final String expectedValues = "[" + Arrays.stream(values.get()).map(StringRepresentable::getSerializedName).collect(Collectors.joining(", ")) + "]";
        return fromStringResolver(id, () -> expectedIdRange, () -> expectedValues, Enum::ordinal, i -> values.get()[i], enumName);
    }

    /**
     * Like {@link StringRepresentable#fromStringResolver(ToIntFunction, IntFunction, Function)} but with named errors.
     */
    public static <E extends StringRepresentable> Codec<E> fromStringResolver(String id, Supplier<String> expectedIdRange, Supplier<String> expectedValues, ToIntFunction<E> objectToIdMapper, IntFunction<E> idToObjectMapper, Function<? super String, ? extends E> nameToObjectMapper)
    {
        return new Codec<>()
        {
            @Override
            public <T> DataResult<T> encode(E input, DynamicOps<T> ops, T prefix)
            {
                return ops.compressMaps() ?
                    ops.mergeToPrimitive(prefix, ops.createInt(objectToIdMapper.applyAsInt(input))) :
                    ops.mergeToPrimitive(prefix, ops.createString(input.getSerializedName()));
            }

            @Override
            public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> ops, T input)
            {
                return ops.compressMaps() ?
                    ops.getNumberValue(input)
                        .flatMap(index -> Optional.ofNullable(idToObjectMapper.apply(index.intValue()))
                            .map(DataResult::success)
                            .orElseGet(() -> DataResult.error("Unknown " + id + " id: " + index + ", expected in " + expectedIdRange.get())))
                        .map(value -> Pair.of(value, ops.empty())) :
                    ops.getStringValue(input)
                        .flatMap(name -> Optional.ofNullable(nameToObjectMapper.apply(name))
                            .map(DataResult::success)
                            .orElseGet(() -> DataResult.error("Unknown " + id + " name: " + name + ", expected one of " + expectedValues.get())))
                        .map(value -> Pair.of(value, ops.empty()));
            }
        };
    }

    /**
     * Replacement for {@link net.minecraft.resources.RegistryFileCodec#create(ResourceKey, Codec)}
     */
    public static <E> Codec<Supplier<E>> registryEntryCodec(ResourceKey<? extends Registry<E>> registryKey, Codec<E> elementCodec)
    {
        return new RegistryEntryCodec<>(registryKey, elementCodec);
    }

    /**
     * Replacement for {@link net.minecraft.resources.RegistryFileCodec#homogeneousList(ResourceKey, Codec)}
     * Note this is <strong>not</strong> a homogeneous list, and rather accepts mixed lists, prioritizing the registry name.
     */
    public static <E> Codec<List<Supplier<E>>> registryEntryListCodec(ResourceKey<? extends Registry<E>> registryKey, Codec<E> elementCodec)
    {
        return either(
            ShapedCodec.likeString(new RegistryEntryCodec<>(registryKey, elementCodec)),
            ShapedCodec.likeMap(elementCodec)
        ).xmap(e -> e.map(e1 -> e1, e2 -> () -> e2), Either::left).listOf();
    }

    public static <T> MapCodec<Supplier<T>> nonNullSupplier(MapCodec<Supplier<T>> codec, String key)
    {
        final Function<Supplier<T>, DataResult<Supplier<T>>> map = nonNullSupplierCheck(key);
        return codec.flatXmap(map, map);
    }

    /**
     * {@link ExtraCodecs#nonNullSupplierCheck()} but with a specific name, and doesn't print the supplier (because really, why would you).
     */
    public static <T> Function<Supplier<T>, DataResult<Supplier<T>>> nonNullSupplierCheck(String key)
    {
        return supplier -> {
            try
            {
                if (supplier.get() == null)
                {
                    return DataResult.error("Missing " + key);
                }
            }
            catch (Exception e)
            {
                return DataResult.error("Invalid " + key + ": " + e.getMessage());
            }
            return DataResult.success(supplier, Lifecycle.stable());
        };
    }

    public static <T> Codec<List<Supplier<T>>> nonNullSupplierList(Codec<List<Supplier<T>>> codec, String key)
    {
        final Function<List<Supplier<T>>, DataResult<List<Supplier<T>>>> map = nonNullSupplierListCheck(key);
        return codec.flatXmap(map, map);
    }

    /**
     * {@link ExtraCodecs#nonNullSupplierListCheck()} but with a specific name and better errors.
     */
    public static <T> Function<List<Supplier<T>>, DataResult<List<Supplier<T>>>> nonNullSupplierListCheck(String key)
    {
        return list -> {
            final List<String> errors = new ArrayList<>();
            for (int i = 0; i < list.size(); ++i)
            {
                final Supplier<T> supplier = list.get(i);
                try
                {
                    if (supplier.get() == null)
                    {
                        errors.add("Missing " + key + " at index " + i);
                    }
                }
                catch (Exception exception)
                {
                    errors.add("Invalid " + key + " at index " + i + ": " + exception.getMessage());
                }
            }
            return !errors.isEmpty() ? DataResult.error(String.join(", ", errors)) : DataResult.success(list, Lifecycle.stable());
        };
    }
}
