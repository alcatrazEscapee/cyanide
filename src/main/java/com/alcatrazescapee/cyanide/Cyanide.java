/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;

import com.mojang.serialization.DataResult;

@Mod("cyanide")
public final class Cyanide
{
    @Nullable public static final String CLIENT_SELF_TEST = System.getProperty("cyanide.client_self_test");
    public static final Logger LOGGER = LogManager.getLogger();

    public static Optional<Method> findClientSelfTestMethod()
    {
        if (CLIENT_SELF_TEST != null)
        {
            try
            {
                String[] parts = CLIENT_SELF_TEST.split("#");
                Class<?> cls = Class.forName(parts[0]);
                return Optional.of(cls.getMethod(parts[1]));
            }
            catch (Exception e)
            {
                LOGGER.error("Unable to locate method from property: " + CLIENT_SELF_TEST, e);
            }
        }
        return Optional.empty();
    }

    public static void invokeAndRethrow(Method method)
    {
        try
        {
            method.invoke(null);
        }
        catch (IllegalAccessException e)
        {
            LOGGER.error("Unable to invoke method: " + method.getDeclaringClass() + "#" + method.getName(), e);
        }
        catch (InvocationTargetException e)
        {
            rethrow(e.getTargetException());
        }
    }

    @SuppressWarnings("unused")
    public static void selfTest()
    {
        LOGGER.info("Cyanide Self Test");
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void rethrow(Throwable t) throws E
    {
        throw (E) t;
    }
}
