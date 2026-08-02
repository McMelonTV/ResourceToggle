package ing.boykiss.resourcetoggle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ToggleConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ToggleConfig loaded;
    public Mode mode = Mode.RAM;
    public boolean keepLoaderPacks = true;
    public boolean toastOnToggle = true;
    public List<String> extraKeepPacks = new ArrayList<>();
    public List<String> extraExcludePacks = new ArrayList<>();

    public static ToggleConfig get() {
        if (loaded == null) {
            loaded = load();
        }
        return loaded;
    }

    public static void reload() {
        loaded = load();
    }

    private static Path configPath() {
        Minecraft minecraft = Minecraft.getInstance();
        Path gameDir = minecraft != null && minecraft.gameDirectory != null
                ? minecraft.gameDirectory.toPath()
                : Path.of(".");
        return gameDir.resolve("config").resolve(ResourceToggle.MOD_ID + ".json");
    }

    private static ToggleConfig load() {
        Path path = configPath();
        ToggleConfig config = null;
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                config = GSON.fromJson(reader, ToggleConfig.class);
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Failed to read {}: {}", path, e.toString());
            }
        }
        if (config == null) {
            config = new ToggleConfig();
            save(config, path);
        }
        return config;
    }

    private static void save(ToggleConfig config, Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to write {}", path, e);
        }
    }

    public enum Mode {
        /**
         * Keep only the vanilla resources on the GPU at all times and swap pointers.
         */
        GPU("gpu"),
        /**
         * Keep both resource stacks fully cached in memory; switch re-uploads to the GPU.
         */
        RAM("ram"),
        /**
         * Perform a regular vanilla-style reload on every switch.
         */
        ON_DEMAND("on_demand");

        private final String key;

        Mode(String key) {
            this.key = key;
        }

        public static Mode byKey(String key) {
            for (Mode mode : values()) {
                if (mode.key.equalsIgnoreCase(key)) {
                    return mode;
                }
            }
            return RAM;
        }

        public String key() {
            return this.key;
        }
    }
}
