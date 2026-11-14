package com.sumirelabs.sodiumleafculling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class LeafCullingConfig implements PerformanceSettingsAccessor {
    private static final Logger LOGGER = Logger.getLogger("seleafculling");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private volatile LeafCullingQuality quality = LeafCullingQuality.SOLID;
    private volatile boolean distanceCulling;
    private volatile int distance = DistanceLeafCulling.DEFAULT_DISTANCE;
    private volatile boolean cullAdjacentBlocks;
    private volatile Set<String> excludedLeafBlocks = Collections.emptySet();

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

    @SuppressWarnings("deprecation") // Older Minecraft versions include Gson before parseReader.
    LeafCullingConfig(Path directory) {
        this.file = directory.resolve("seleafculling.json");
        try {
            if (Files.exists(this.file)) {
                try (Reader reader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
                    loadValues(new JsonParser().parse(reader).getAsJsonObject());
                }
            } else {
                save();
            }
        } catch (IOException | RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Could not load Leaf Culling settings", ex);
        }
    }

    private static String readValue(JsonObject values, String key, String fallback) {
        JsonElement value = values.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : fallback;
    }

    private void loadValues(JsonObject values) {
        try {
            quality = LeafCullingQuality.valueOf(readValue(values, "leaf_culling_quality", "SOLID"));
        } catch (IllegalArgumentException ignored) { }
        distanceCulling = Boolean.parseBoolean(readValue(values, "distance_leaf_culling", "false"));
        try {
            distance = DistanceLeafCulling.clampDistance(Integer.parseInt(
                    readValue(values, "leaf_culling_distance", Integer.toString(distance))));
        } catch (NumberFormatException ignored) { }
        cullAdjacentBlocks = Boolean.parseBoolean(readValue(values, "cull_adjacent_blocks", "false"));
        JsonElement exclusions = values.get("excluded_leaf_blocks");
        if (exclusions != null && exclusions.isJsonArray()) {
            Set<String> ids = new LinkedHashSet<>();
            for (JsonElement element : exclusions.getAsJsonArray()) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    ids.add(element.getAsString());
                }
            }
            setExcludedLeafBlocks(ids);
        }
    }

    public synchronized void save() {
        JsonObject values = new JsonObject();
        values.addProperty("leaf_culling_quality", quality.name());
        values.addProperty("distance_leaf_culling", distanceCulling);
        values.addProperty("leaf_culling_distance", distance);
        values.addProperty("cull_adjacent_blocks", cullAdjacentBlocks);
        JsonArray exclusions = new JsonArray();
        for (String id : excludedLeafBlocks) exclusions.add(id);
        values.add("excluded_leaf_blocks", exclusions);
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "seleafculling-", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(values, writer);
                writer.write('\n');
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
    public Set<String> getExcludedLeafBlocks() { return excludedLeafBlocks; }
    public void setExcludedLeafBlocks(Collection<String> ids) {
        Set<String> copy = new LinkedHashSet<>();
        for (String id : ids) {
            if (id != null && !id.trim().isEmpty()) copy.add(id.trim());
        }
        excludedLeafBlocks = copy.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(copy);
    }
}
