package ing.boykiss.resourcetoggle.mixin;

import ing.boykiss.resourcetoggle.ToggleController;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Intercepts every client resource reload (F3+T, server pack pushes, world
 * joins and our own toggle) and substitutes the pack list when vanilla-only
 * mode is active. The repository selection itself is never touched, so the
 * state the server observes stays identical.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftReloadMixin {
    @Redirect(
            method = "reloadResourcePacks(ZLnet/minecraft/client/GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;openAllSelected()Ljava/util/List;")
    )
    private List<PackResources> resourcetoggle$filterReloadPacks(PackRepository repository) {
        return ToggleController.INSTANCE.resolveReloadPacks(repository);
    }
}
