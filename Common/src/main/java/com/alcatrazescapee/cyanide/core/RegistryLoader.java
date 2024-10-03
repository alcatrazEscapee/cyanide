package com.alcatrazescapee.cyanide.core;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import com.alcatrazescapee.cyanide.mixin.accessor.MappedRegistryAccessor;
import com.alcatrazescapee.cyanide.platform.XPlatform;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryDataLoader.RegistryData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import static com.alcatrazescapee.cyanide.mixin.accessor.RegistryDataLoaderAccessor.*;

/**
 * Cyanide's rewrite of {@link RegistryDataLoader}, with an increased emphasis on proper error reporting.
 * <p>
 * <strong>N.B.</strong> this implementation has to respect both NeoForge patches, and Fabric API mixins which allow mods to provide
 * datapack registries.
 */
public final class RegistryLoader
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * @see RegistryDataLoader#load(ResourceManager, RegistryAccess, List)
     */
    public static RegistryAccess.Frozen load(
        ResourceManager resourceManager,
        RegistryAccess registryAccess,
        List<RegistryData<?>> registryData
    )
    {
        final ErrorReporter reporter = new ErrorReporter();
        final List<Loader<?>> registryLoader = registryData.stream().<Loader<?>>map(Loader::new).toList();
        final Map<ResourceKey<? extends Registry<?>>, RegistryOps.RegistryInfo<?>> registryLookup = new IdentityHashMap<>();
        final RegistryOps.RegistryInfoLookup lookup = new RegistryOps.RegistryInfoLookup() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey)
            {
                return Optional.ofNullable((RegistryOps.RegistryInfo<T>) registryLookup.get(registryKey));
            }
        };

        // Trigger Fabric's callback before any loading is complete
        final Map<ResourceKey<? extends Registry<?>>, Registry<?>> registryMap = new IdentityHashMap<>(registryData.size());
        registryLoader.forEach(loader -> registryMap.put(loader.registry.key(), loader.registry));
        XPlatform.INSTANCE.postFabricBeforeRegistryLoadEvent(registryMap);

        // Populate lookup with both static registries, and dynamic (datapack) ones
        // Create new empty registries for the dynamic ones, and record them in the top-level registry map
        registryAccess.registries().forEach(entry -> registryLookup.put(entry.key(), invoke$createInfoForContextRegistry(entry.value())));
        registryLoader.forEach(entry -> registryLookup.put(entry.data.key(), invoke$createInfoForNewRegistry(entry.registry)));

        // Load each registry content sequentially
        registryLoader.forEach(loader -> loadRegistry(resourceManager, registryAccess, lookup, loader, reporter));

        // Attempt to freeze registries. This will fail if there are unbound elements in the registry, which we should be able to handle
        // gracefully, because we know what causes these errors
        registryLoader.forEach(loader -> freezeRegistry(loader, reporter));

        // At this point, we have collected all errors. If any have been raised, we build and print an informative error with the
        // causes.
        if (reporter.hasError())
        {
            throw new IllegalStateException("Failed to load registries due to errors");
        }

        // Bake registries - everything should be fine at this point
        final List<? extends WritableRegistry<?>> registries = registryLoader.stream()
            .map(Loader::registry)
            .toList();

        return new RegistryAccess.ImmutableRegistryAccess(registries).freeze();
    }

    /**
     * @see RegistryDataLoader#loadContentsFromManager
     */
    private static <T> void loadRegistry(
        ResourceManager resourceManager,
        RegistryAccess registryAccess,
        RegistryOps.RegistryInfoLookup lookup,
        Loader<T> loader,
        ErrorReporter reporter
    )
    {
        final String registryPath = Registries.elementsDirPath(loader.registry.key());
        final FileToIdConverter converter = FileToIdConverter.json(registryPath);
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, lookup);
        final Function<Optional<KnownPack>, RegistrationInfo> infoCache = accessor$getRegistrationInfoCache();

        // Modify the element codec to add conditions, as per NeoForge's patch
        Decoder<Optional<T>> decoder = XPlatform.INSTANCE.getNeoForgeConditionalCodec(loader.data.elementCodec());

        for (Map.Entry<ResourceLocation, Resource> entry : converter.listMatchingResources(resourceManager).entrySet())
        {
            final ResourceLocation id = entry.getKey();
            final ResourceKey<T> key = ResourceKey.create(loader.registry.key(), converter.fileToId(id));
            final Resource resource = entry.getValue();
            final RegistrationInfo info = infoCache.apply(resource.knownPackInfo());

            final JsonElement json;
            try (Reader reader = resource.openAsReader())
            {
                json = JsonParser.parseReader(reader);
            }
            catch (JsonSyntaxException e)
            {
                reporter.loadingErrors.put(key, "Syntax error: " + e.getMessage());
                continue;
            }
            catch (JsonIOException | IOException e)
            {
                reporter.loadingErrors.put(key, "IO Error: " + e.getMessage());
                continue;
            }

            // Before parsing, consider conditions. Both loaders implement some variant of them.
            // - Fabric implements conditions as a basic check on the JSON itself
            // - NeoForge implements conditions using a wrapped decoder
            //
            // So, we support both
            if (XPlatform.INSTANCE.checkFabricConditions(json, key, registryAccess))
            {
                continue;
            }

            // The optional will be null if NeoForge's conditions fail to pass
            // In this case, we log the same message, otherwise we register the element
            final DataResult<Optional<T>> result = decoder.parse(ops, json);
            result.ifSuccess(candidate ->
                candidate.ifPresentOrElse(
                    value -> loader.registry.register(key, value, info),
                    () -> LOGGER.debug("Skipping loading registry entry {} as its conditions were not met", key)
                ));
            result.ifError(error -> reporter.loadingErrors.put(key, "Parsing error: " + error.message()));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void freezeRegistry(Loader<T> loader, ErrorReporter reporter)
    {
        // Registry freezing has two conditions:
        // 1. Check that all holders are bound,
        // 2. Check that there are no unregistered intrusive holders
        //
        // For the first check, we can do it more gracefully by checking manually, as we can classify and
        // exclude errors based on i.e. if we already know something failed to parse, any references to it will be broken.
        //
        // For the second, this is only caused by a mod failing to register something - not a datapack. So we assume this won't
        // occur, and have minimal error handling for this case.
        ((MappedRegistryAccessor<T>) loader.registry).accessor$byKey()
            .entrySet()
            .stream()
            // Exclude unbound value errors that were caused by an element we already have an associated loading error from,
            // as the loading error is likely much more informative
            .filter(e -> !e.getValue().isBound() && !reporter.loadingErrors.containsKey(e.getKey()))
            .forEach(e -> reporter.unboundErrors.add(e.getKey()));

        try
        {
            loader.registry.freeze();
        }
        catch (IllegalStateException e)
        {
            final String message = e.getMessage();
            if (message.startsWith("Some intrusive holders were not registered"))
            {
                // Intrusive holder error - this is a mod error
                reporter.registryErrors.put(loader.data.key(), "Likely Mod Error: " + e.getMessage());
            }
            else if (!message.startsWith("Unbound values in registry"))
            {
                // Unknown error - try as best we can to raise it
                reporter.registryErrors.put(loader.data.key(), "Unknown Error: " + e.getMessage());
            }
            // If we hit an unbound registry error, we must have marked the unbound elements earlier,
            // either because they were hit with a loading error, or added to the unbound errors
            //
            // So, we don't need to do anything here for that case
        }

        if (
            loader.data.requiredNonEmpty() &&
            loader.registry.isEmpty() &&
            // We don't already have an error for this registry
            !reporter.registryErrors.containsKey(loader.data.key()) &&
            // No error was present when loading this registry
            reporter.registryErrors
                .keySet()
                .stream()
                .noneMatch(e -> e.registryKey().equals(loader.data.key()))
        )
        {
            reporter.registryErrors.put(loader.data.key(), "Empty registry: " + loader.data.key().location());
        }
    }

    record Loader<T>(RegistryData<T> data, WritableRegistry<T> registry)
    {
        Loader(RegistryData<T> data)
        {
            this(data, new MappedRegistry<>(data.key(), Lifecycle.stable()));
        }
    }

    record ErrorReporter(
        Map<ResourceKey<?>, String> loadingErrors,
        List<ResourceKey<?>> unboundErrors,
        Map<ResourceKey<? extends Registry<?>>, String> registryErrors
    )
    {
        ErrorReporter()
        {
            this(new IdentityHashMap<>(), new ArrayList<>(), new IdentityHashMap<>());
        }

        boolean hasError()
        {
            return !loadingErrors.isEmpty()
                || !unboundErrors.isEmpty()
                || !registryErrors.isEmpty();
        }
    }
}
