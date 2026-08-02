package ing.boykiss.resourcetoggle;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.Util;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core toggle logic.
 *
 * <p>The client's resource pack repository (and therefore the state the server
 * sees) is never modified. Instead the pack list handed to the resource reload
 * is filtered: when vanilla-only mode is active, every non-vanilla, non-mod
 * pack is dropped from the reload while the repository still considers it
 * selected. This means the server keeps receiving the usual
 * "pack downloaded / applied" reports while only the client's rendering
 * switches to the vanilla set.
 */
public final class ToggleController {
    public static final ToggleController INSTANCE = new ToggleController();
    private static final Logger LOGGER = LogUtils.getLogger();
    /**
     * Pack ids of vanilla built-in packs (excluded from the vanilla-only stack).
     */
    private static final Set<String> BUILT_IN_PACK_IDS = Set.of("programmer_art", "high_contrast", "debug");

    /**
     * Byte caches shared across reloads, keyed by pack id.
     */
    private final Map<String, Map<String, byte[]>> packCaches = new ConcurrentHashMap<>();

    private volatile boolean vanillaOnly = false;
    private volatile boolean warming = false;

    private ToggleController() {
    }

    public boolean isVanillaOnly() {
        return this.vanillaOnly;
    }

    public Component buttonLabel() {
        return Component.translatable(
                this.vanillaOnly ? "resourcetoggle.button.vanilla" : "resourcetoggle.button.all"
        );
    }

    /**
     * Called from the pause menu button, the title screen button and the hotkey.
     */
    public synchronized void toggle() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gui == null) {
            return;
        }
        if (minecraft.gui.overlay() instanceof LoadingOverlay) {
            LOGGER.debug("Resource reload already in progress, ignoring toggle");
            return;
        }
        this.vanillaOnly = !this.vanillaOnly;
        ToggleConfig config = ToggleConfig.get();
        if (config.toastOnToggle) {
            minecraft.gui.hud.setOverlayMessage(
                    Component.translatable(
                            this.vanillaOnly ? "resourcetoggle.toast.vanilla" : "resourcetoggle.toast.all"
                    ),
                    true
            );
        }
        CompletableFuture<Void> reload = minecraft.reloadResourcePacks();
        if (config.mode != ToggleConfig.Mode.ON_DEMAND) {
            reload.thenRunAsync(this::warmInactiveStack, Util.backgroundExecutor());
        }
    }

    /**
     * Called by the {@code Minecraft} mixin whenever a resource reload is about
     * to build its pack list. Returns the packs that should actually be loaded.
     */
    public List<PackResources> resolveReloadPacks(PackRepository repository) {
        List<Pack> selected = List.copyOf(repository.getSelectedPacks());
        if (!this.vanillaOnly) {
            return repository.openAllSelected();
        }
        LOGGER.debug("Vanilla-only mode active, filtering {} packs", selected.size());
        ToggleConfig config = ToggleConfig.get();
        List<PackResources> result = new ArrayList<>(selected.size());
        for (Pack pack : selected) {
            if (!shouldInclude(pack, config)) {
                continue;
            }
            PackResources opened = pack.open();
            result.add(wrap(opened));
        }
        return result;
    }

    private boolean shouldInclude(Pack pack, ToggleConfig config) {
        String id = pack.getId();
        if (config.extraExcludePacks.contains(id)) {
            return false;
        }
        if (config.extraKeepPacks.contains(id)) {
            return true;
        }
        if ("vanilla".equals(id)) {
            return true;
        }
        if (!config.keepLoaderPacks) {
            return false;
        }
        // User packs live under "file/", server packs under "server/".
        if (id.startsWith("file/") || id.startsWith("server/")) {
            return false;
        }
        if (BUILT_IN_PACK_IDS.contains(id)) {
            return false;
        }
        // Anything else is provided by the mod loader.
        return true;
    }

    private PackResources wrap(PackResources opened) {
        if (ToggleConfig.get().mode == ToggleConfig.Mode.ON_DEMAND) {
            return opened;
        }
        Map<String, byte[]> cache = this.packCaches.computeIfAbsent(opened.packId(), id -> new ConcurrentHashMap<>());
        return new CachedPackResources(opened, cache);
    }

    /**
     * Preloads the stack that would be used after the next toggle into memory,
     * so that subsequent switches avoid disk and zip access entirely.
     */
    private void warmInactiveStack() {
        if (this.warming) {
            return;
        }
        this.warming = true;
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) {
                return;
            }
            ToggleConfig config = ToggleConfig.get();
            if (config.mode == ToggleConfig.Mode.ON_DEMAND) {
                return;
            }
            List<Pack> selected = List.copyOf(minecraft.getResourcePackRepository().getSelectedPacks());
            for (Pack pack : selected) {
                if (!this.vanillaOnly && !shouldInclude(pack, config)) {
                    // We are in full mode: only the packs that survive the toggle matter.
                    continue;
                }
                PackResources opened = pack.open();
                try {
                    CachedPackResources cached = new CachedPackResources(
                            opened,
                            this.packCaches.computeIfAbsent(opened.packId(), id -> new ConcurrentHashMap<>())
                    );
                    cached.warm();
                } finally {
                    opened.close();
                }
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to preload resource stack", e);
        } finally {
            this.warming = false;
        }
    }

    public void clearCaches() {
        this.packCaches.clear();
    }
}
