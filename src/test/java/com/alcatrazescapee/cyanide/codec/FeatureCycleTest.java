/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import org.junit.jupiter.api.*;


public class FeatureCycleTest extends TestHelper implements FeatureCycleDSL
{
    @Test
    public void testCycleDuplicateAdjacentFeature()
    {
        build(
            biome("biome",
                feature(0, "duplicate"),
                feature(0, "duplicate")
            )
        ).error(
            """
            A feature cycle was found.
                        
            Cycle:
            At step 0
            Feature 'duplicate'
              must be before 'duplicate' (defined in 'biome' at index 0, 1)
            """
        ).expectCycleX4();
    }

    @Test
    public void testCycleDuplicateNonAdjacentFeature()
    {
        build(
            biome("biome",
                feature(0, "duplicate"),
                feature(0, "innocent"),
                feature(0, "duplicate")
            )
        ).error(
            """
            A feature cycle was found.
                            
            Cycle:
            At step 0
            Feature 'duplicate'
              must be before 'innocent' (defined in 'biome' at index 0, 1)
              must be before 'duplicate' (defined in 'biome' at index 1, 2)
            """
        ).expectCycleX4();
    }

    @Test
    public void testCycleMultipleBiomes()
    {
        build(
            biome("biome1_2",
                feature(0, "target1"),
                feature(0, "dummy1"),
                feature(0, "target2")
            ),
            biome("biome2_1",
                feature(0, "target2"),
                feature(0, "dummy2"),
                feature(0, "target1")
            ),
            biome("biome3",
                feature(0, "dummy2"),
                feature(0, "diversion"),
                feature(0, "dummy1")
            )
        ).error(
            """
            A feature cycle was found.
                        
            Cycle:
            At step 0
            Feature 'target1'
              must be before 'dummy1' (defined in 'biome1_2' at index 0, 1)
              must be before 'target2' (defined in 'biome1_2' at index 1, 2)
              must be before 'dummy2' (defined in 'biome2_1' at index 0, 1)
              must be before 'target1' (defined in 'biome2_1' at index 1, 2)
            """
        ).expectCycleX4();
    }

    @Test
    public void testCycleEqualFeatureData()
    {
        build(
            biome("biome_1",
                feature(0, "dummy1"),
                feature(0, "dummy2"),
                feature(0, "firstIndex2"),
                feature(0, "secondIndex1")
            ),
            biome("biome_2",
                feature(0, "dummy1"),
                feature(0, "dummy2"),
                feature(0, "secondIndex2"),
                feature(0, "firstIndex1")
            ),
            biome("biome_3",
                feature(0, "firstIndex1"),
                feature(0, "firstIndex2")
            ),
            biome("biome_4",
                feature(0, "secondIndex1"),
                feature(0, "secondIndex2")
            )
        ).error(
            """
            A feature cycle was found.
                            
            Cycle:
            At step 0
            Feature 'firstIndex2'
              must be before 'secondIndex1' (defined in 'biome_1' at index 2, 3)
              must be before 'secondIndex2' (defined in 'biome_4' at index 0, 1)
              must be before 'firstIndex1' (defined in 'biome_2' at index 2, 3)
              must be before 'firstIndex2' (defined in 'biome_3' at index 0, 1)
            """
        ).expectCycleX4();
    }

    @Test
    public void testNoCycleDuplicateButDifferentStep()
    {
        build(
            biome("biome_same_feature_different_steps",
                feature(0, "innocent"),
                feature(1, "innocent")
            ))
            .expectNoCycleX4();
    }

    @Test
    public void testNoCycleEqualFeatureData()
    {
        build(
            biome("biome_1",
                feature(0, "first"),
                feature(0, "second")
            ),
            biome("biome_2",
                feature(0, "second"),
                feature(0, "third")
            ))
            .expectNoCycle()
            .expectNoCycleInVanilla()
            .identityFeatures()
            .expectNoCycle()
            .expectVanillaCycle(); // MC-252369 : https://bugs.mojang.com/browse/MC-252369
    }
}
