package com.alcatrazescapee.cyanide.codec;

import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImprovedListCodecTests
{
    @Test
    public void testListCodec()
    {
        runExamples("List Codec", Codec.STRING.listOf());
    }

    @Test
    public void testImprovedListCodec()
    {
        runExamples("Improved List Codec", Codecs.list(Codec.STRING, (e, i) -> e + " at index " + i));
    }

    private void runExamples(String name, Codec<List<String>> codec)
    {
        System.out.println("Examples: " + name);

        final DynamicOps<JsonElement> ops = JsonOps.INSTANCE;

        final JsonArray array = new JsonArray();
        array.add("first");
        array.add("second");
        array.add("third");

        final JsonArray arrayInvalidElements = new JsonArray();
        arrayInvalidElements.add("first");
        arrayInvalidElements.add(101);
        arrayInvalidElements.add("second");
        arrayInvalidElements.add(new JsonObject());
        arrayInvalidElements.add("third");

        final DataResult<Pair<List<String>, JsonElement>> decode = codec.decode(ops, array);
        final DataResult<Pair<List<String>, JsonElement>> decodeInvalidElements = codec.decode(ops, arrayInvalidElements);

        assertTrue(decode.result().isPresent());
        assertEquals(Arrays.asList("first", "second", "third"), decode.result().get().getFirst());

        assertTrue(decodeInvalidElements.error().isPresent());
        System.out.println(decodeInvalidElements.error().get().message());
    }
}
