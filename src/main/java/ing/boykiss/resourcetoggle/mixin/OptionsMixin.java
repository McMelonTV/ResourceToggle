package ing.boykiss.resourcetoggle.mixin;

import ing.boykiss.resourcetoggle.ResourceToggle;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Registers the toggle hotkey on both loaders without any loader API:
 * the {@code keyMappings} array is final, so the accessor strips the
 * final modifier via {@code @Mutable}.
 */
@Mixin(Options.class)
public abstract class OptionsMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void resourcetoggle$registerKeyMapping(CallbackInfo ci) {
        Options options = (Options) (Object) this;
        KeyMapping[] keyMappings = options.keyMappings;
        KeyMapping[] extended = new KeyMapping[keyMappings.length + 1];
        System.arraycopy(keyMappings, 0, extended, 0, keyMappings.length);
        extended[keyMappings.length] = ResourceToggle.TOGGLE_KEY;
        ((OptionsAccessor) (Object) this).resourcetoggle$setKeyMappings(extended);
    }
}
