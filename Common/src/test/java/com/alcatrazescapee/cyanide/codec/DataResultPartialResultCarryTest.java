package com.alcatrazescapee.cyanide.codec;

import net.minecraft.util.Unit;

import com.mojang.serialization.DataResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataResultPartialResultCarryTest
{
    @Test
    public void testPartialResultCarryDefaultBehavior()
    {
        DataResult<Unit> result = DataResult.success(Unit.INSTANCE);
        for (String key : new String[] {"first", "second", "third"})
        {
            result = result.flatMap(r -> DataResult.error(() -> key + " not found").map(u -> r));
        }

        assertTrue(result.error().isPresent());
        assertEquals("first not found", result.error().get().message());
    }

    @Test
    public void testPartialResultCarryImprovedBehavior()
    {
        DataResult<Unit> result = DataResult.success(Unit.INSTANCE);
        for (String key : new String[] {"first", "second", "third"})
        {
            result = result.flatMap(r -> DataResult.error(() -> key + " not found").map(u -> r)).setPartial(Unit.INSTANCE);
        }

        assertTrue(result.error().isPresent());
        assertEquals("first not found; second not found; third not found", result.error().get().message());
    }
}
