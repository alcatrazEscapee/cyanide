/*
 * Part of the Cyanide mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.cyanide.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

record Data(String name, int id)
{
    static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("name").forGetter(Data::name),
        Codec.INT.fieldOf("id").forGetter(Data::id)
    ).apply(instance, Data::new));
}
