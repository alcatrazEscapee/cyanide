package com.alcatrazescapee.cyanide.codec;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.mutable.MutableInt;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.*;

public final class FeatureCycleDetector
{
    public static List<FeatureSorter.StepFeatureData> buildFeaturesPerStep(List<Holder<Biome>> allBiomes, Function<Holder<Biome>, List<HolderSet<PlacedFeature>>> biomeFeatures)
    {
        return buildFeaturesPerStep(allBiomes, biomeFeatures, biome -> "???", feature -> "???");
    }

    /**
     * A modified version of {@link FeatureSorter#buildFeaturesPerStep(List, Function, boolean)} with several improvements, in order to properly report errors.
     * Added comments, removed vanilla's slow as heck "try this again by removing biomes until it doesn't break" detector and replace with one that is able to track the detected cycle during the DFS.
     *
     * @param biomeName A function to name biomes in error messages
     * @param featureName A function to name features in error messages
     *
     * @throws FeatureCycleException if a feature was detected.
     */
    @SuppressWarnings("ConstantConditions")
    public static List<FeatureSorter.StepFeatureData> buildFeaturesPerStep(List<Holder<Biome>> allBiomes, Function<Holder<Biome>, List<HolderSet<PlacedFeature>>> biomeFeatures, Function<Biome, String> biomeName, Function<PlacedFeature, String> featureName)
    {
        // The second method parameter to this method is if it was called recursively / from itself or not. We ignore it entirely
        boolean calledFromTopLevel = true;

        // Maps to establish identity among features and biomes
        // We assign features and biomes ID numbers based on ==, and then create wrapper objects which respect equals() identity
        final Reference2IntMap<PlacedFeature> featureToIntIdMap = new Reference2IntOpenHashMap<>();
        final Reference2IntMap<Biome> biomeToIntIdMap = new Reference2IntOpenHashMap<>();
        final MutableInt nextFeatureId = new MutableInt(0);
        final MutableInt nextBiomeId = new MutableInt(0);

        // Sort by step, then by index
        final Comparator<FeatureData> compareByStepThenByIndex = Comparator.comparingInt(FeatureData::step).thenComparingInt(FeatureData::featureId);
        final Map<FeatureData, Set<FeatureData>> nodesToChildren = new TreeMap<>(compareByStepThenByIndex);

        int maxSteps = 0;

        // Trace of where FeatureData's are found, in reference to biomes, indexes, and steps
        final Map<FeatureData, Map<BiomeData, IntSet>> nodesToTracebacks = new HashMap<>();

        for (Holder<Biome> holder : allBiomes)
        {
            // Loop through all biomes
            // For each biome, we ultimately compute the maxSteps - the maximum number of generation steps of any biome
            // We then take the features in the biome, and, treating (step, index) as an absolute ordering, add them to a flat list of FeatureData
            // We compute integer IDs for each placed feature as we go through them.
            // At the end, once we've computed this list, we then have that F1, F2, ... Fn, where Fj must come before Fi if j < i
            // So, we represent that as a graph F1 -> F2 -> ... -> Fn

            final Biome biome = holder.value();
            final List<FeatureData> flatDataList = new ArrayList<>();
            final List<HolderSet<PlacedFeature>> features = biomeFeatures.apply(holder);

            maxSteps = Math.max(maxSteps, features.size());

            for (int stepIndex = 0; stepIndex < features.size(); ++stepIndex)
            {
                int biomeIndex = 0;
                for (Holder<PlacedFeature> supplier : features.get(stepIndex))
                {
                    final PlacedFeature feature = supplier.value();
                    final FeatureData featureIdentity = new FeatureData(idFor(feature, featureToIntIdMap, nextFeatureId), stepIndex, feature);
                    flatDataList.add(featureIdentity);

                    // Track traceback biomes
                    final BiomeData biomeIdentity = new BiomeData(idFor(biome, biomeToIntIdMap, nextBiomeId), biome);

                    nodesToTracebacks
                        .computeIfAbsent(featureIdentity, key -> new HashMap<>(1))
                        .computeIfAbsent(biomeIdentity, key -> new IntOpenHashSet())
                        .add(biomeIndex);
                    biomeIndex++;
                }
            }

            for (int featureIndex = 0; featureIndex < flatDataList.size(); ++featureIndex)
            {
                final Set<FeatureData> children = nodesToChildren.computeIfAbsent(flatDataList.get(featureIndex), key -> new TreeSet<>(compareByStepThenByIndex));
                if (featureIndex < flatDataList.size() - 1)
                {
                    children.add(flatDataList.get(featureIndex + 1));
                }
            }
        }

        final Set<FeatureData> nonCyclicalNodes = new TreeSet<>(compareByStepThenByIndex);
        final Set<FeatureData> inProgress = new TreeSet<>(compareByStepThenByIndex);
        final List<FeatureData> sortedFeatureData = new ArrayList<>();
        List<FeatureData> featureCycle = new ArrayList<>();

        // Loop through every known node in the graph (the graph may not be connected, since all we know is it's a series of strings)
        // F1,1 -> F1,2 -> ... F1,n1
        // ...
        // Fm,1 -> Fm,2 -> ... Fm,nm
        // Where some Fi,j may be the same (which would create connected components)
        for (FeatureData node : nodesToChildren.keySet())
        {
            if (!inProgress.isEmpty())
            {
                throw new IllegalStateException("You somehow broke the universe; DFS bork (iteration finished with non-empty in-progress vertex set");
            }

            // In vanilla, the DFS returns true to indicate a cycle exists
            // The rest of this if branch is then just code to try and narrow down the error in an extremely unhelpful way
            if (!nonCyclicalNodes.contains(node) && depthFirstSearch(nodesToChildren, nonCyclicalNodes, inProgress, sortedFeatureData::add, featureCycle::add, node))
            {
                if (featureCycle.size() <= 1)
                {
                    throw new IllegalStateException("There was a feature cycle that involved 0 or 1 feature??");
                }

                // Trim the cycle - the last feature should occur somewhere in the cycle. This is done before reversing, so last = index 0
                final FeatureData loop = featureCycle.get(0);
                for (int i = 1; i < featureCycle.size(); i++)
                {
                    if (featureCycle.get(i).equals(loop))
                    {
                        // Reset the cycle by trimming off all excess after this index
                        featureCycle = featureCycle.subList(0, i + 1);
                        break;
                    }
                }

                // Reverse the cycle, since we add to it in reverse order
                Collections.reverse(featureCycle);

                // At this point, we have enough information to throw a custom exception, with the biomes and features involved
                // if (true) here, as we keep the below code since it's from vanilla, but it is never executed
                if (true) throw new FeatureCycleException(nodesToTracebacks, featureCycle, biomeName, featureName);

                // A cycle has been found from the DFS
                if (!calledFromTopLevel)
                {
                    // This is a cheap way of exiting the below try { catch } recursive call
                    throw new IllegalStateException("Feature order cycle found");
                }

                // This is the most quick and dirty way of trying to find what biomes are involved in a feature cycle
                // We start with the list of all biomes, and collectively try and remove one biome at a time, redo the ENTIRE feature cycle detection, until we cannot remove any.
                final List<Holder<Biome>> biomeSubset = new ArrayList<>(allBiomes);
                int biomesLeft;
                do
                {
                    biomesLeft = biomeSubset.size();
                    ListIterator<Holder<Biome>> biomeSubsetIterator = biomeSubset.listIterator();

                    // This iterator is O(n^2) for n = number of biomes
                    // In the best case, we do O(n) calls, and make zero removals (or, do O(n) calls removing the first one every time)
                    while (biomeSubsetIterator.hasNext())
                    {
                        Holder<Biome> biome = biomeSubsetIterator.next();
                        biomeSubsetIterator.remove(); // Consider if we remove this biome

                        try
                        {
                            // Then, we try and build features again, but without this biome
                            // If this passes, we include the biome again below.
                            // If it fails, the boolean above means that the "false" will just immediately throw an exception
                            buildFeaturesPerStep(biomeSubset, biomeFeatures, biomeName, featureName);
                        }
                        catch (IllegalStateException e)
                        {
                            // Continue, we were able to remove this biome
                            // So, skip re-adding it and keep trying to narrow down the biome list
                            continue;
                        }
                        biomeSubsetIterator.add(biome);
                    }
                } while (biomesLeft != biomeSubset.size());

                throw new IllegalStateException("Feature order cycle found, involved biomes: " + biomeSubset);
            }
        }

        Collections.reverse(sortedFeatureData);

        // This is the intended result: A list just of features to be applied at each given generation step
        final ImmutableList.Builder<FeatureSorter.StepFeatureData> featuresPerStepData = ImmutableList.builder();

        for (int stepIndex = 0; stepIndex < maxSteps; ++stepIndex)
        {
            final int finalStepIndex = stepIndex;
            final List<PlacedFeature> featuresAtStep = sortedFeatureData.stream()
                .filter(arg -> arg.step() == finalStepIndex)
                .map(FeatureData::feature)
                .collect(Collectors.toList());

            final int totalIndex = featuresAtStep.size();
            final Object2IntMap<PlacedFeature> featureToIndexMapping = new Object2IntOpenCustomHashMap<>(totalIndex, Util.identityStrategy());
            for (int index = 0; index < totalIndex; ++index)
            {
                featureToIndexMapping.put(featuresAtStep.get(index), index);
            }

            featuresPerStepData.add(new FeatureSorter.StepFeatureData(featuresAtStep, featureToIndexMapping));
        }

        return featuresPerStepData.build();
    }

    public static boolean depthFirstSearch(Map<FeatureData, Set<FeatureData>> edges, Set<FeatureData> nonCyclicalNodes, Set<FeatureData> pathSet, Consumer<FeatureData> onNonCyclicalNodeFound, Consumer<FeatureData> onCyclicalNodeFound, FeatureData start)
    {
        // Called initially with:
        // nonCyclicalNodes = { ... } does not contain start
        // reachable = {}
        // start = ?

        // Returning true must (somehow) indicate the presence of a cycle
        // nonCyclicalNodes = the nodes that we've already seen, and verified that they have no cycles
        // reachable = an empty set (when this is first called), representing all edges that are reachable.
        if (nonCyclicalNodes.contains(start))
        {
            // The DFS has already seen the current node, so return false
            return false;
        }
        else if (pathSet.contains(start))
        {
            // If we have navigated back to an element that we haven't seen (and so verified that no cycles from that node exist)
            // But, we know it's in the path set, then there's a cycle!
            // We then push these through the consumer in reverse order, as any true return value will bubble up
            onCyclicalNodeFound.accept(start);
            return true;
        }
        else
        {
            pathSet.add(start); // This node is now in the path set (we consider it as a node that may have a cycle, and want to mark if we can reach back to it)
            for (FeatureData next : edges.getOrDefault(start, ImmutableSet.of()))
            {
                if (depthFirstSearch(edges, nonCyclicalNodes, pathSet, onNonCyclicalNodeFound, onCyclicalNodeFound, next))
                {
                    // A cycle was found, so we exit here.
                    // We append the cycle node found in verse order.
                    onCyclicalNodeFound.accept(start);
                    return true;
                }
            }

            // At this point: we cannot find a way back into the path set, considering 'start' as a stepping stone node. (Since above, recursively, we've DFS'd all the way that we can go from this node.)
            // As a result, we can
            // 1. mark this as not in the path set
            // 2. mark this node as already seen, and valid
            // And then return false (which exits this recursive call of the DFS)
            pathSet.remove(start);
            nonCyclicalNodes.add(start);
            onNonCyclicalNodeFound.accept(start);
            return false;
        }
    }

    private static <T> int idFor(T object, Reference2IntMap<T> objectToIntIdMap, MutableInt nextId)
    {
        return objectToIntIdMap.computeIfAbsent(object, key -> nextId.getAndIncrement());
    }

    private static String buildErrorMessage(Map<FeatureData, Map<BiomeData, IntSet>> tracebacks, List<FeatureData> cycle, Function<Biome, String> biomeName, Function<PlacedFeature, String> featureName)
    {
        final StringBuilder error = new StringBuilder("""
                A feature cycle was found.

                Cycle:
                """);

        final ListIterator<FeatureData> iterator = cycle.listIterator();
        final FeatureData start = iterator.next();
        Map<BiomeData, IntSet> prevTracebacks = tracebacks.get(start);
        error.append("At step ")
            .append(start.step)
            .append('\n')
            .append("Feature '")
            .append(featureName.apply(start.feature))
            .append("'\n");

        while (iterator.hasNext())
        {
            final FeatureData current = iterator.next();
            final Map<BiomeData, IntSet> currentTracebacks = tracebacks.get(current);
            int found = 0;
            for (BiomeData biome : Sets.intersection(prevTracebacks.keySet(), currentTracebacks.keySet()))
            {
                // Check if the features have a relative ordering from prev -> current, in that biome
                final int prevTb = prevTracebacks.get(biome).intStream().min().orElseThrow();
                final int currTb = currentTracebacks.get(biome).intStream().max().orElseThrow();
                if (prevTb < currTb)
                {
                    if (found == 0)
                    {
                        error.append("  must be before '")
                            .append(featureName.apply(current.feature))
                            .append("' (defined in '")
                            .append(biomeName.apply(biome.biome))
                            .append("' at index ")
                            .append(prevTb)
                            .append(", ")
                            .append(currTb);
                    }
                    found++;
                }
            }
            if (found > 1)
            {
                error.append(" and ")
                    .append(found - 1)
                    .append(" others)\n");
            }
            else if (found > 0)
            {
                error.append(")\n");
            }

            prevTracebacks = currentTracebacks;
        }
        return error.toString();
    }

    /**
     * @param featureId An integer ID mapping for the feature, NOT the index of the feature within the step
     * @param step The step index the feature was found in.
     * @param feature The placed feature itself
     */
    record FeatureData(int featureId, int step, PlacedFeature feature) {}

    /**
     * @param biomeId An integer ID mapping for the biome
     * @param biome The biome itself
     */
    record BiomeData(int biomeId, Biome biome) {}

    static class FeatureCycleException extends RuntimeException
    {
        public FeatureCycleException(Map<FeatureData, Map<BiomeData, IntSet>> tracebacks, List<FeatureData> cycle, Function<Biome, String> biomeName, Function<PlacedFeature, String> featureName)
        {
            super(buildErrorMessage(tracebacks, cycle, biomeName, featureName));
        }
    }
}
