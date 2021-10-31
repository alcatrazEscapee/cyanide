/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReportingCodecTests
{
    @Test
    public void testReportingCodec()
    {
        final Codec<Data> codec = Codecs.reporting(RecordCodecBuilder.create(instance -> instance.group(
            Codecs.reporting(Codec.STRING.fieldOf("name"), "String name for Data object").forGetter(Data::name),
            Codecs.reporting(Codec.INT.fieldOf("id"), "integer ID for Data object").forGetter(Data::id)
        ).apply(instance, Data::new)), "Data object");

        runExamples("Codec.reporting", codec);
    }

    @Test
    public void testNormalCodec()
    {
        runExamples("RecordCodecBuilder", Data.CODEC);
    }

    private void runExamples(String name, Codec<Data> codec)
    {
        System.out.println("Examples: " + name);

        final DynamicOps<JsonElement> ops = JsonOps.INSTANCE;

        final JsonObject json = new JsonObject();
        json.addProperty("name", "dummy");
        json.addProperty("id", 101);

        final JsonObject jsonInvalidName = new JsonObject();
        jsonInvalidName.add("name", new JsonObject());
        jsonInvalidName.addProperty("id", 101);

        final JsonObject jsonInvalidId = new JsonObject();
        jsonInvalidId.addProperty("name", "invalid dummy");

        final DataResult<Pair<Data, JsonElement>> decode = codec.decode(ops, json);
        final DataResult<Pair<Data, JsonElement>> decodeInvalidName = codec.decode(ops, jsonInvalidName);
        final DataResult<Pair<Data, JsonElement>> decodeInvalidId = codec.decode(ops, jsonInvalidId);

        assertTrue(decode.result().isPresent());
        assertEquals(new Data("dummy", 101), decode.result().get().getFirst());

        assertTrue(decodeInvalidName.error().isPresent());
        System.out.println(decodeInvalidName.error().get().message());

        assertTrue(decodeInvalidId.error().isPresent());
        System.out.println(decodeInvalidId.error().get().message());
    }
}
