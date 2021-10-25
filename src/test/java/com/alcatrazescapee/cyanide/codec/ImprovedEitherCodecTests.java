package com.alcatrazescapee.cyanide.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ImprovedEitherCodecTests
{
    static record Data(String name, int id)
    {
        static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Data::name),
            Codec.INT.fieldOf("id").forGetter(Data::id)
        ).apply(instance, Data::new));
    }

    @Test
    public void testEitherCodec()
    {
        runExamples(Codec.either(Data.CODEC, Codec.STRING));
    }

    @Test
    public void testEitherCodecInverse()
    {
        runExamples(Codec.either(Codec.STRING, Data.CODEC).xmap(Either::swap, Either::swap));
    }

    @Test
    public void testImprovedEitherCodec()
    {
        runExamples(Codecs.either(ShapedCodec.likeMap(Data.CODEC), ShapedCodec.likeString(Codec.STRING)));
    }

    @Test
    public void testImprovedEitherCodecInverse()
    {
        runExamples(Codecs.either(ShapedCodec.likeString(Codec.STRING), ShapedCodec.likeMap(Data.CODEC)).xmap(Either::swap, Either::swap));
    }

    private void runExamples(Codec<Either<Data, String>> codec)
    {
        final DynamicOps<JsonElement> ops = JsonOps.INSTANCE;

        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", "dummy");
        jsonObject.addProperty("id", 101);

        final JsonObject jsonObjectInvalid = new JsonObject();
        jsonObjectInvalid.addProperty("name", "invalid dummy");

        final JsonElement jsonString = new JsonPrimitive("dummy");
        final JsonElement jsonStringInvalid = new JsonPrimitive(101);

        final DataResult<Pair<Either<Data, String>, JsonElement>> decodeJsonObject = codec.decode(ops, jsonObject);
        final DataResult<Pair<Either<Data, String>, JsonElement>> decodeJsonObjectInvalid = codec.decode(ops, jsonObjectInvalid);
        final DataResult<Pair<Either<Data, String>, JsonElement>> decodeJsonString = codec.decode(ops, jsonString);
        final DataResult<Pair<Either<Data, String>, JsonElement>> decodeJsonStringInvalid = codec.decode(ops, jsonStringInvalid);

        assertTrue(decodeJsonObject.result().isPresent());
        assertTrue(decodeJsonObject.result().get().getFirst().left().isPresent());
        assertEquals(new Data("dummy", 101), decodeJsonObject.result().get().getFirst().left().get());

        assertTrue(decodeJsonObjectInvalid.error().isPresent());
        System.out.println(decodeJsonObjectInvalid.error().get().message());

        assertTrue(decodeJsonString.result().isPresent());
        assertTrue(decodeJsonString.result().get().getFirst().right().isPresent());
        assertEquals("dummy", decodeJsonString.result().get().getFirst().right().get());

        assertTrue(decodeJsonStringInvalid.error().isPresent());
        System.out.println(decodeJsonStringInvalid.error().get().message());
    }
}
