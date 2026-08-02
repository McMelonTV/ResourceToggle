package ing.boykiss.resourcetoggle.mixin;

import ing.boykiss.resourcetoggle.ResourceToggle;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftTickMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void resourcetoggle$onClientTick(CallbackInfo ci) {
        ResourceToggle.onClientTick();
    }
}
