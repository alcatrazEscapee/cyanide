package com.alcatrazescapee.cyanide.mixin.accessor;

import java.util.Optional;
import java.util.function.Function;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.repository.KnownPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * These exist to minimize the need to copy sections of vanilla code, which raises issues with patches (both Forge and Fabric)
 * in the area of code we want to interact with. Where we can, minimal copying is ideal.
 */
@Mixin(RegistryDataLoader.class)
public interface RegistryDataLoaderAccessor
{
    @Accessor("REGISTRATION_INFO_CACHE")
    static Function<Optional<KnownPack>, RegistrationInfo> accessor$getRegistrationInfoCache()
    {
        throw new AssertionError();
    }

    @Invoker("createInfoForContextRegistry")
    static <T> RegistryOps.RegistryInfo<T> invoke$createInfoForContextRegistry(HolderLookup.RegistryLookup<T> registryLookup)
    {
        throw new AssertionError();
    }

    @Invoker("createInfoForNewRegistry")
    static <T> RegistryOps.RegistryInfo<T> invoke$createInfoForNewRegistry(WritableRegistry<T> registry)
    {
        throw new AssertionError();
    }
}
