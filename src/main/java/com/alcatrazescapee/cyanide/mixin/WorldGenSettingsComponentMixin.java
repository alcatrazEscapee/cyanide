package com.alcatrazescapee.cyanide.mixin;

import java.util.Optional;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import net.minecraft.client.gui.screens.worldselection.WorldGenSettingsComponent;
import net.minecraft.world.level.levelgen.WorldGenSettings;

import com.alcatrazescapee.cyanide.codec.MixinHooks;
import com.mojang.serialization.DataResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldGenSettingsComponent.class)
public abstract class WorldGenSettingsComponentMixin
{
    @Redirect(method = "updateDataPacks", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/DataResult;resultOrPartial(Ljava/util/function/Consumer;)Ljava/util/Optional;", remap = false))
    private Optional<WorldGenSettings> resultOrPartialWithImprovedErrorMessages(DataResult<WorldGenSettings> result, Consumer<String> onError)
    {
        return MixinHooks.printWorldGenSettingsError(result);
    }
}
