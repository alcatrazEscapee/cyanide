package com.alcatrazescapee.cyanide.mixin.client;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin
{
    @Redirect(
        method = "lambda$applyNewPackConfig$17",
        at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Throwable;)V")
    )
    private void preventPrintingExceptionInErrorMessage(Logger logger, String message, Throwable throwable)
    {
        logger.warn(message); // Don't dump the stack trace, because it's meaningless
        logger.warn(throwable.getMessage());
    }
}
