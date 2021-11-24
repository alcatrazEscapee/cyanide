/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import java.lang.reflect.Field;

import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import static org.junit.jupiter.api.Assertions.fail;

public interface TestHelper
{
    static void bootstrap()
    {
        try
        {
            Field field = SharedConstants.class.getDeclaredField("CURRENT_VERSION");
            field.setAccessible(true);
            field.set(null, DetectedVersion.BUILT_IN);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            fail("Unable to set SharedConstants#CURRENT_VERSION", e);
        }

        Bootstrap.bootStrap();
    }
}
