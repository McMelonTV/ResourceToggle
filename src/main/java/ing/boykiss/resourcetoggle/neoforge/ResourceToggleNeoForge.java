package ing.boykiss.resourcetoggle.neoforge;

import ing.boykiss.resourcetoggle.ResourceToggle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = ResourceToggle.MOD_ID, dist = Dist.CLIENT)
public final class ResourceToggleNeoForge {
    public ResourceToggleNeoForge() {
        ResourceToggle.init();
    }
}
