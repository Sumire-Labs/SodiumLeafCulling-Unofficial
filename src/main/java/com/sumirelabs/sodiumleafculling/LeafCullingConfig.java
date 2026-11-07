package com.sumirelabs.sodiumleafculling;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
//?} elif forge {
/*import net.minecraftforge.fml.loading.FMLPaths;
*///?} else {
/*import net.neoforged.fml.loading.FMLPaths;
*///?}

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LeafCullingConfig implements PerformanceSettingsAccessor {
    private static final Logger LOGGER = Logger.getLogger("seleafculling");
    private final Path file;
    private volatile LeafCullingQuality quality = LeafCullingQuality.SOLID;
    private volatile boolean distanceCulling;
    private volatile int distance = DistanceLeafCulling.DEFAULT_DISTANCE;
    private volatile boolean cullAdjacentBlocks;

    private static final class Holder {
        private static final LeafCullingConfig INSTANCE = new LeafCullingConfig(configDirectory());
    }

    public static LeafCullingConfig get() {
        return Holder.INSTANCE;
    }

    private static Path configDirectory() {
        //? if fabric {
        return FabricLoader.getInstance().getConfigDir();
        //?} else {
        /*return FMLPaths.CONFIGDIR.get();
        *///?}
    }

    LeafCullingConfig(Path directory) {
        this.file = directory.resolve("seleafculling.cfg");
        try {
            if (Files.exists(this.file)) {
                Properties properties = new Properties();
                try (Reader reader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
                loadValues(properties);
            } else {
                //? if embeddium || embeddium_modern {
                /*Path embeddium = directory.resolve("embeddium-options.json");
                migrate(Files.isRegularFile(embeddium) ? embeddium : directory.resolve("sodium-options.json"));
                *///?} else {
                migrate(directory.resolve("sodium-options.json"));
                //?}
                save();
            }
        } catch (IOException | RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Could not load Leaf Culling settings", ex);
        }
    }

    @SuppressWarnings("deprecation") // Older Minecraft versions include Gson before parseReader.
    private void migrate(Path legacy) throws IOException {
        if (!Files.isRegularFile(legacy)) return;
        try (Reader reader = Files.newBufferedReader(legacy, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            Properties values = new Properties();
            // Early Sodium versions kept the leaf settings under advanced.
            for (String category : new String[]{"advanced", "performance"}) {
                if (!root.has(category) || !root.get(category).isJsonObject()) continue;
                JsonObject section = root.getAsJsonObject(category);
                for (String key : new String[]{"leaf_culling_quality", "distance_leaf_culling", "leaf_culling_distance"}) {
                    if (section.has(key) && section.get(key).isJsonPrimitive())
                        values.setProperty(key, section.get(key).getAsString());
                }
            }
            loadValues(values);
        }
    }

    private void loadValues(Properties properties) {
        try {
            quality = LeafCullingQuality.valueOf(properties.getProperty("leaf_culling_quality", "SOLID"));
        } catch (IllegalArgumentException ignored) { }
        distanceCulling = Boolean.parseBoolean(properties.getProperty("distance_leaf_culling"));
        try {
            distance = DistanceLeafCulling.clampDistance(Integer.parseInt(
                    properties.getProperty("leaf_culling_distance", Integer.toString(distance))));
        } catch (NumberFormatException ignored) { }
        cullAdjacentBlocks = Boolean.parseBoolean(properties.getProperty("cull_adjacent_blocks"));
    }

    public synchronized void save() {
        Properties values = new Properties();
        values.setProperty("leaf_culling_quality", quality.name());
        values.setProperty("distance_leaf_culling", Boolean.toString(distanceCulling));
        values.setProperty("leaf_culling_distance", Integer.toString(distance));
        values.setProperty("cull_adjacent_blocks", Boolean.toString(cullAdjacentBlocks));
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "seleafculling-", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                values.store(writer, "Sodium/Embeddium Leaf Culling");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Could not save Leaf Culling settings", ex);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    @Override public LeafCullingQuality sodiumleafculling$getQuality() { return quality; }
    @Override public void sodiumleafculling$setQuality(LeafCullingQuality value) { quality = value; }
    @Override public boolean sodiumleafculling$isDistanceCullingEnabled() { return distanceCulling; }
    @Override public void sodiumleafculling$setDistanceCullingEnabled(boolean value) { distanceCulling = value; }
    @Override public int sodiumleafculling$getDistance() { return distance; }
    @Override public void sodiumleafculling$setDistance(int value) { distance = DistanceLeafCulling.clampDistance(value); }
    public boolean cullAdjacentBlocks() { return cullAdjacentBlocks; }
    public void setCullAdjacentBlocks(boolean value) { cullAdjacentBlocks = value; }
}
