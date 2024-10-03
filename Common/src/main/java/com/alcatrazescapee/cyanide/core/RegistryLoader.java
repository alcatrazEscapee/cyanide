package com.alcatrazescapee.cyanide.core;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
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
import net.minecraft.tags.TagKey;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
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
    private static final Pattern PATTERN_LINE = Pattern.compile("at line (\\d+) column (\\d+)");

    /**
     * @see RegistryDataLoader#load(ResourceManager, RegistryAccess, List)
     */
    public static RegistryAccess.Frozen load(
        ResourceManager resourceManager,
        RegistryAccess registryAccess,
        List<RegistryData<?>> registryData
    )
    {
        final Reporter reporter = new Reporter();
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
        //
        // Note that we want to use a custom registration lookup for new registries - this allows us to track when elements are being
        // referenced, and then resolve these references as they get defined, rather than only knowing after the fact what element
        // was trying to reference something that never got defined.
        registryAccess.registries().forEach(entry -> registryLookup.put(entry.key(), invoke$createInfoForContextRegistry(entry.value())));
        registryLoader.forEach(entry -> registryLookup.put(entry.data.key(), createNewRegistryInfo(entry.registry, reporter)));

        // Load each registry content sequentially
        registryLoader.forEach(loader -> loadRegistry(resourceManager, registryAccess, lookup, loader, reporter));

        // Attempt to freeze registries. This will fail if there are unbound elements in the registry, which we should be able to handle
        // gracefully, because we know what causes these errors
        registryLoader.forEach(loader -> freezeRegistry(loader, reporter));

        // At this point, we have collected all errors. If any have been raised, we build and print an informative error with the
        // causes.
        if (reporter.hasError())
        {
            throw new IllegalStateException(reporter.logErrors());
        }

        // Bake registries - everything should be fine at this point
        final List<? extends WritableRegistry<?>> registries = registryLoader.stream()
            .map(Loader::registry)
            .toList();

        return new RegistryAccess.ImmutableRegistryAccess(registries).freeze();
    }

    /**
     * @see MappedRegistry#createRegistrationLookup()
     */
    private static <T> RegistryOps.RegistryInfo<T> createNewRegistryInfo(WritableRegistry<T> registry, Reporter reporter)
    {
        final var originalInfo = invoke$createInfoForNewRegistry(registry);
        final var originalLookup = originalInfo.getter();
        return new RegistryOps.RegistryInfo<>(originalInfo.owner(), new HolderGetter<>() {
            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> key)
            {
                // The original will always do this when lookup up, leaving unbound values later
                return Optional.of(getOrThrow(key));
            }

            @Override
            public Holder.Reference<T> getOrThrow(ResourceKey<T> key)
            {
                // This is the core location where we are able to track "unbound holders in registry"
                // When a holder gets referenced, it's in one of two states: unbound or bound
                // 1. If it is bound, this element has already been resolved, meaning we don't need to track it
                // 2. If it is un-bound, we record the source context that we accessed this with (the resource key of the
                //    accessor, and resource).
                //
                // Later, as we register elements, we remove any elements from out "unbound references" list that get
                // registered. In the end, we should have a map of any unbound holders to the location(s) where they got
                // referenced!
                final Holder.Reference<T> holder = originalLookup.getOrThrow(key);
                if (reporter.currentReference != null && !holder.isBound())
                {
                    // A source reference is known, and we are accessing an unbound holder, so make sure we record the
                    // reference here
                    reporter.unboundReferences.computeIfAbsent(key, k -> new ArrayList<>()).add(reporter.currentReference);
                }
                return holder;
            }

            @Override
            public Optional<HolderSet.Named<T>> get(TagKey<T> tagKey)
            {
                return originalLookup.get(tagKey);
            }

            @Override
            public HolderSet.Named<T> getOrThrow(TagKey<T> tagKey)
            {
                return originalLookup.getOrThrow(tagKey);
            }
        }, originalInfo.elementsLifecycle());
    }

    /**
     * @see RegistryDataLoader#loadContentsFromManager
     */
    private static <T> void loadRegistry(
        ResourceManager resourceManager,
        RegistryAccess registryAccess,
        RegistryOps.RegistryInfoLookup lookup,
        Loader<T> loader,
        Reporter reporter
    )
    {
        final String registryPath = Registries.elementsDirPath(loader.registry.key());
        final FileToIdConverter converter = FileToIdConverter.json(registryPath);
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, lookup);
        final Function<Optional<KnownPack>, RegistrationInfo> infoCache = accessor$getRegistrationInfoCache();
        final RegistryReporter registryReporter = reporter.registry(loader.data.key());

        // Modify the element codec to add conditions, as per NeoForge's patch
        Decoder<Optional<T>> decoder = XPlatform.INSTANCE.getNeoForgeConditionalCodec(loader.data.elementCodec());

        for (Map.Entry<ResourceLocation, Resource> entry : converter.listMatchingResources(resourceManager).entrySet())
        {
            final ResourceLocation id = entry.getKey();
            final ResourceKey<T> key = ResourceKey.create(loader.registry.key(), converter.fileToId(id));
            final Resource resource = entry.getValue();
            final RegistrationInfo info = infoCache.apply(resource.knownPackInfo());

            // Populate the unbound reference, as we are able to track any references this element makes
            reporter.currentReference = new UnboundReference(resource, key);

            final JsonElement json;
            try (Reader reader = resource.openAsReader())
            {
                json = JsonParser.parseReader(reader);
            }
            catch (JsonSyntaxException e)
            {
                // If the JSON was malformed, we try and extract a line number from the output, and we print the JSON if we can
                final StringBuilder error = new StringBuilder();

                // JsonParser.parseReader wraps everything, so grab the cause for the more relevant error
                error.append("Syntax Error: %s\n".formatted(e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));

                try (Reader reader = resource.openAsReader())
                {
                    final List<String> rawText = IOUtils.readLines(reader);
                    final Matcher match = PATTERN_LINE.matcher(e.getMessage());

                    if (match.find())
                    {
                        final int lineNo = Integer.parseInt(match.group(1));
                        final int columnNo = Integer.parseInt(match.group(2));

                        final String spacing = " ".repeat(Math.max(columnNo - 2, 0));
                        final List<String> contextLines = rawText.subList(Math.max(0, lineNo - 5), Math.min(rawText.size(), lineNo));

                        error.append("  at:\n%s\n%s^\n%shere\n".formatted(
                            String.join("\n", contextLines),
                            spacing,
                            spacing
                        ));
                    }
                }
                catch (IOException o)
                {
                    // Unable to read raw text
                }

                registryReporter.loadingErrors.put(key, new LoadingError(resource.sourcePackId(), error.toString()));
                continue;
            }
            catch (JsonIOException | IOException e)
            {
                registryReporter.loadingErrors.put(key, new LoadingError(resource.sourcePackId(), "IO Error: " + e.getMessage()));
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
            result.ifError(error -> registryReporter.loadingErrors.put(key, new LoadingError(
                resource.sourcePackId(),
                "Parsing Error: " + error.message()
            )));

            // In addition to registering the object, we need to remove it from a list of possibly unbound references
            // This prevents us from hanging on to various other references when we know the object exists
            //
            // Note that we do this **even if there was a loading error**. Why? Because if there was a loading error,
            // we would already have a more accurate error than "unbound holder", because we know it was at least a file
            // we were trying to load in the first place.
            reporter.unboundReferences.remove(key);
            reporter.currentReference = null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void freezeRegistry(Loader<T> loader, Reporter reporter)
    {
        final RegistryReporter registryReporter = reporter.registry(loader.data.key());

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
            .filter(e -> !e.getValue().isBound() && !registryReporter.loadingErrors.containsKey(e.getKey()))
            .forEach(e -> {
                final StringBuilder error = new StringBuilder();

                error.append("Missing File Error: '%s' was referenced but not defined\n".formatted(prettyId(e.getKey())));

                // Consult the map of unbound references to figure out who was referencing this object
                reporter.unboundReferences.getOrDefault(e.getKey(), List.of()).forEach(ref -> error
                    .append(at(ref.resource.sourcePackId(), ref.key))
                    .append("\n"));

                registryReporter.unboundErrors.add(error.toString());
            });

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
                registryReporter.miscErrors.add("Likely Mod Error: " + e.getMessage());
            }
            else if (!message.startsWith("Unbound values in registry"))
            {
                // Unknown error - try as best we can to raise it
                registryReporter.miscErrors.add("Unknown Error: " + e.getMessage());
            }
            // If we hit an unbound registry error, we must have marked the unbound elements earlier,
            // either because they were hit with a loading error, or added to the unbound errors
            //
            // So, we don't need to do anything here for that case
        }

        // Only report empty registry errors if there were no loading errors for the registry,
        // as this would then be a knock on effect
        if (loader.data.requiredNonEmpty() && loader.registry.isEmpty() && registryReporter.loadingErrors.isEmpty())
        {
            registryReporter.miscErrors.add("Empty registry: " + loader.data.key().location());
        }
    }

    /**
     * Returns a pretty-printed reference for a resource key. For example, {@code "namespace:worldgen/configured_feature/path"}
     */
    private static ResourceLocation prettyId(ResourceKey<?> key)
    {
        return key.location().withPrefix(key.registry().getPath() + "/");
    }

    private static String at(String sourcePackId, ResourceKey<?> key)
    {
        return "  at '%s' defined in '%s'".formatted(prettyId(key), sourcePackId);
    }

    record Loader<T>(RegistryData<T> data, WritableRegistry<T> registry)
    {
        Loader(RegistryData<T> data)
        {
            this(data, new MappedRegistry<>(data.key(), Lifecycle.stable()));
        }
    }

    static class Reporter
    {
        final Map<ResourceKey<? extends Registry<?>>, RegistryReporter> errors = new IdentityHashMap<>();
        final Map<ResourceKey<?>, List<UnboundReference>> unboundReferences = new IdentityHashMap<>();

        @Nullable UnboundReference currentReference = null;

        RegistryReporter registry(ResourceKey<? extends Registry<?>> key)
        {
            return errors.computeIfAbsent(key, k -> new RegistryReporter(new IdentityHashMap<>(), new ArrayList<>(), new ArrayList<>()));
        }

        boolean hasError()
        {
            return errors.values()
                .stream()
                .anyMatch(RegistryReporter::hasError);
        }

        String logErrors()
        {
            final StringBuilder log = new StringBuilder();

            log.append("\n\n===== An error occurred loading registries =====\n\n");
            errors.forEach((key, reporter) -> {
                if (reporter.hasError())
                {
                    reporter.logErrors(log, key);
                }
            });

            return log.toString();
        }
    }

    record RegistryReporter(
        Map<ResourceKey<?>, LoadingError> loadingErrors,
        List<String> unboundErrors,
        List<String> miscErrors
    )
    {
        boolean hasError()
        {
            return !loadingErrors.isEmpty()
                || !unboundErrors.isEmpty()
                || !miscErrors.isEmpty();
        }

        void logErrors(StringBuilder log, ResourceKey<? extends Registry<?>> key)
        {
            log.append("In registry '%s':\n\n".formatted(key.location()));

            loadingErrors.entrySet()
                .stream()
                .sorted(Comparator.comparing(e -> e.getKey().location()))
                .forEach(entry -> log
                    .append(entry.getValue().message)
                    .append("\n")
                    .append(at(entry.getValue().sourcePackId, entry.getKey()))
                    .append("\n\n"));

            unboundErrors.forEach(error -> log
                .append(error)
                .append("\n"));

            miscErrors.forEach(error -> log
                .append(error)
                .append("\n"));

            log.append("-----\n\n");
        }
    }

    record UnboundReference(Resource resource, ResourceKey<?> key) {}
    record LoadingError(String sourcePackId, String message) {}
}
