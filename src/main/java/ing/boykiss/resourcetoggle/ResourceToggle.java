package ing.boykiss.resourcetoggle;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

public final class ResourceToggle {
    public static final String MOD_ID = "resourcetoggle";

    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.resourcetoggle.toggle",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KeyMapping.Category.MISC
    );

    private ResourceToggle() {
    }

    /**
     * Called from the loader entrypoints.
     */
    public static void init() {
        ToggleConfig.get();
    }

    /**
     * Called from the {@code Minecraft.tick} mixin.
     */
    public static void onClientTick() {
        while (TOGGLE_KEY.consumeClick()) {
            ToggleController.INSTANCE.toggle();
        }
    }

    /**
     * Adds the toggle button to a screen (title / pause).
     */
    public static void addToggleButton(Screen screen, java.util.function.Consumer<Button> adder) {
        Button button = Button.builder(ToggleController.INSTANCE.buttonLabel(), btn -> {
            ToggleController.INSTANCE.toggle();
            btn.setMessage(ToggleController.INSTANCE.buttonLabel());
        }).bounds(screen.width - 106, 4, 102, 20).build();
        adder.accept(button);
    }
}
