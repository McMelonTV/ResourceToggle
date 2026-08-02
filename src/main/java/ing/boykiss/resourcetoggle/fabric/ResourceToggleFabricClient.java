package ing.boykiss.resourcetoggle.fabric;

import ing.boykiss.resourcetoggle.ResourceToggle;
import net.fabricmc.api.ClientModInitializer;

public final class ResourceToggleFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ResourceToggle.init();
    }
}
