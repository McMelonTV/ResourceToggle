package ing.boykiss.resourcetoggle.mixin;

import ing.boykiss.resourcetoggle.ResourceToggle;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the toggle button to the title screen and the pause screen. The hook
 * lives on {@link Screen} because {@code addRenderableWidget} is protected on
 * the superclass, which is not shadowable from subclasses.
 */
@Mixin(Screen.class)
public abstract class ScreenButtonMixin {
    @Shadow
    protected abstract GuiEventListener addRenderableWidget(GuiEventListener widget);

    @Inject(method = "init(II)V", at = @At("RETURN"))
    private void resourcetoggle$addToggleButton(CallbackInfo ci) {
        if ((Object) this instanceof TitleScreen || (Object) this instanceof PauseScreen) {
            ResourceToggle.addToggleButton((Screen) (Object) this, button -> this.addRenderableWidget(button));
        }
    }
}
