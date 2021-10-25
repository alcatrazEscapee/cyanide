/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;

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
