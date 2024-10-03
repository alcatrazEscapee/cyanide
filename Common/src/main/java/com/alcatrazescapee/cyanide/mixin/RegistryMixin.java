package com.alcatrazescapee.cyanide.mixin;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Registry.class)
public interface RegistryMixin
{
    /**
     * Replace this error message, because it's generically terrible.
     */
    @Inject(
        method =
#if PLATFORM_FABRIC
            "method_57067"
#else
            "lambda$referenceHolderWithLifecycle$2"
#endif
        ,
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void missingElementInRegistryError(ResourceLocation id, CallbackInfoReturnable<String> cir)
    {
        cir.setReturnValue("Unknown registry key in " + ((Registry<?>) this).key().location() + ": '" + id + "'");
    }
}
