package com.alcatrazescapee.cyanide.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImprovedOptionalCodecTests
{
    record Box(String inside) {}

    @Test
    public void testOptionalFieldOf()
    {
        runExamples("Codec.optionalFieldOf", Codec.STRING.optionalFieldOf("inside", "default"));
    }

    @Test
    public void testImprovedOptionalFieldOf()
    {
        runExamples("ImprovedOptionalCodec", Codecs.optionalFieldOf(Codec.STRING, "inside", "default"));
    }

    private void runExamples(String name, MapCodec<String> mapCodec)
    {
        final Codec<Box> codec = mapCodec.codec().xmap(Box::new, Box::inside);

        System.out.println("Examples: " + name);

        final DynamicOps<JsonElement> ops = JsonOps.INSTANCE;

        final JsonObject jsonBoxNoInside = new JsonObject();
        final JsonObject jsonBoxValidInside = new JsonObject();
        jsonBoxValidInside.addProperty("inside", "candy");

        final JsonObject jsonBoxInvalidInside = new JsonObject();
        jsonBoxInvalidInside.addProperty("inside", 3);

        final DataResult<Pair<Box, JsonElement>> decodeNoInside = codec.decode(ops, jsonBoxNoInside);
        final DataResult<Pair<Box, JsonElement>> decodeValidInside = codec.decode(ops, jsonBoxValidInside);
        final DataResult<Pair<Box, JsonElement>> decodeInvalidInside = codec.decode(ops, jsonBoxInvalidInside);

        assertTrue(decodeNoInside.result().isPresent());
        assertEquals("default", decodeNoInside.result().get().getFirst().inside());

        assertTrue(decodeValidInside.result().isPresent());
        assertEquals("candy", decodeValidInside.result().get().getFirst().inside());

        decodeInvalidInside.result().ifPresent(result -> {
            System.out.println("Invalid inside = default value");
            assertEquals("default", result.getFirst().inside());
        });

        decodeInvalidInside.error().ifPresent(error -> {
            System.out.println("Invalid inside = error");
            System.out.println(error.message());
        });
    }
}
