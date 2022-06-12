package com.alcatrazescapee.cyanide.mixin;

import java.net.Proxy;
import com.mojang.datafixers.DataFixer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.alcatrazescapee.cyanide.codec.MixinHooks;
import com.alcatrazescapee.cyanide.platform.FabricPlatform;

/**
 * Fabric only: captures the server as it is created
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin
{
    @Inject(method = "<init>", at = @At("TAIL"))
    private void captureServerOnStart(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer dataFixer, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory, CallbackInfo ci)
    {
        FabricPlatform.setServer(MixinHooks.cast(this));
    }
}
