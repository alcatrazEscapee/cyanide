package com.alcatrazescapee.cyanide.mixin.client;

import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.levelgen.WorldGenSettings;

import com.alcatrazescapee.cyanide.codec.MixinHooks;
import com.mojang.serialization.DataResult;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin
{
    @Dynamic("Lambda method in Minecraft#createLevel")
    @Redirect(method = "*(Lnet/minecraft/core/RegistryAccess$RegistryHolder;Lnet/minecraft/world/level/levelgen/WorldGenSettings;Lnet/minecraft/world/level/LevelSettings;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/core/RegistryAccess$RegistryHolder;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/world/level/DataPackConfig;)Lnet/minecraft/world/level/storage/WorldData;", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/DataResult;resultOrPartial(Ljava/util/function/Consumer;)Ljava/util/Optional;", remap = false))
    private static Optional<WorldGenSettings> resultOrPartialWithImprovedErrorMessage(DataResult<WorldGenSettings> result, Consumer<String> onError)
    {
        return MixinHooks.printWorldGenSettingsError(result);
    }
}
