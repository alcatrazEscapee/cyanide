package com.alcatrazescapee.cyanide.platform;

import java.util.Optional;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;

public final class NeoForgePlatform implements XPlatform
{
    @Override
    public <T> Decoder<Optional<T>> getNeoForgeConditionalCodec(Codec<T> codec)
    {
        return ConditionalOps.createConditionalCodec(NeoForgeExtraCodecs.decodeOnly(codec));
    }
}
