package ing.boykiss.resourcetoggle.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import ing.boykiss.resourcetoggle.ResourceToggle;
import ing.boykiss.resourcetoggle.ToggleController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks the vanilla global key press mechanism (the same one that powers F11
 * fullscreen and F2 screenshots), so the toggle hotkey works on every screen
 * and in-game, but never fires while the player is rebinding keys in the
 * controls screen or while the F3 debug modifier is held.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftGlobalKeyMixin {
    @Inject(method = "handleGlobalKeyPress", at = @At("HEAD"), cancellable = true)
    private void resourcetoggle$onGlobalKeyPress(InputConstants.Key key, boolean controlDown, CallbackInfoReturnable<Boolean> cir) {
        if (ResourceToggle.TOGGLE_KEY.matches(key)) {
            ToggleController.INSTANCE.toggle();
            cir.setReturnValue(true);
        }
    }
}
