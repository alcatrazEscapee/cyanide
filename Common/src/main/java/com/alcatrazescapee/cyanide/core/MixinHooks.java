package com.alcatrazescapee.cyanide.core;

import net.minecraft.world.level.levelgen.WorldGenSettings;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public final class MixinHooks
{
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object o)
    {
        return (T) o;
    }
}
