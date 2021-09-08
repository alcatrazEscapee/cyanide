package com.alcatrazescapee.cyanide.mixin.client;

import net.minecraft.client.Minecraft;

import com.alcatrazescapee.cyanide.Cyanide;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin
{
    /**
     * Injection spot to run client self tests, once resources are loaded
     */
    @Dynamic("Lambda method in Minecraft::new")
    @Inject(method = "*(Ljava/lang/String;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fmlclient/ClientModLoader;completeModLoading()Z", remap = false))
    private void runSelfTest(String s, int i, CallbackInfo ci)
    {
        Cyanide.findClientSelfTestMethod().ifPresent(Cyanide::invokeAndRethrow);
    }
}
