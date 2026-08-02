package ing.boykiss.resourcetoggle;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link PackResources} wrapper that keeps the raw bytes of every resource
 * of the wrapped pack in memory. A switch between the two resource stacks can
 * then happen without touching the disk, zip files or the network.
 */
public final class CachedPackResources implements PackResources {
    private final PackResources delegate;
    private final Map<String, byte[]> cache;
    private final AtomicBoolean closed = new AtomicBoolean();

    public CachedPackResources(PackResources delegate) {
        this(delegate, new ConcurrentHashMap<>());
    }

    public CachedPackResources(PackResources delegate, Map<String, byte[]> cache) {
        this.delegate = delegate;
        this.cache = cache;
    }

    private static String key(PackType type, Identifier location) {
        return type.name() + "/" + location;
    }

    /**
     * The shared byte cache backing this wrapper.
     */
    public Map<String, byte[]> cache() {
        return this.cache;
    }

    @Override
    @Nullable
    public IoSupplier<InputStream> getResource(PackType type, Identifier location) {
        String key = key(type, location);
        byte[] bytes = this.cache.get(key);
        if (bytes != null) {
            return () -> new ByteArrayInputStream(bytes);
        }
        IoSupplier<InputStream> supplier = this.delegate.getResource(type, location);
        if (supplier != null && !this.closed.get()) {
            try (InputStream in = supplier.get()) {
                this.cache.put(key, in.readAllBytes());
                return () -> new ByteArrayInputStream(this.cache.get(key));
            } catch (IOException e) {
                return supplier;
            }
        }
        return supplier;
    }

    @Override
    @Nullable
    public IoSupplier<InputStream> getRootResource(String... path) {
        return this.delegate.getRootResource(path);
    }

    @Override
    public void listResources(PackType type, String namespace, String directory, ResourceOutput output) {
        this.delegate.listResources(type, namespace, directory, output);
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return this.delegate.getNamespaces(type);
    }

    @Override
    @Nullable
    public <T> T getMetadataSection(MetadataSectionType<T> metadataSerializer) throws IOException {
        return this.delegate.getMetadataSection(metadataSerializer);
    }

    @Override
    public PackLocationInfo location() {
        return this.delegate.location();
    }

    @Override
    public void close() {
        this.closed.set(true);
        this.delegate.close();
    }

    /**
     * Reads every client resource of the wrapped pack into the in-memory cache
     * so that the next reload of this stack does not touch disk.
     */
    public void warm() {
        for (String namespace : this.delegate.getNamespaces(PackType.CLIENT_RESOURCES)) {
            this.delegate.listResources(
                    PackType.CLIENT_RESOURCES,
                    namespace,
                    "",
                    (location, supplier) -> {
                        String key = key(PackType.CLIENT_RESOURCES, location);
                        if (this.cache.containsKey(key) || this.closed.get()) {
                            return;
                        }
                        try (InputStream in = supplier.get()) {
                            this.cache.put(key, in.readAllBytes());
                        } catch (IOException ignored) {
                        }
                    }
            );
        }
    }
}
