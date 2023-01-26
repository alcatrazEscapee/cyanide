package com.alcatrazescapee.cyanide.mixin;

import java.util.List;
import com.alcatrazescapee.cyanide.codec.MixinHooks;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RegistryDataLoader.class)
public abstract class RegistryDataLoaderMixin
{
    @Inject(method = "load", at = @At("HEAD"), cancellable = true)
    private static void loadAndReportErrors(ResourceManager resourceManager, RegistryAccess registryAccess, List<RegistryDataLoader.RegistryData<?>> registryData, CallbackInfoReturnable<RegistryAccess.Frozen> cir)
    {
        cir.setReturnValue(MixinHooks.loadAllRegistryData(resourceManager, registryAccess, registryData));
    }
}
