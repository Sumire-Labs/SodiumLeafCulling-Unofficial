package com.sumirelabs.sodiumleafculling;

//? if fabric {
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.bawnorton.mixinsquared.api.MixinCanceller;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

public class LeafCullingMixinCanceller implements MixinCanceller {
    // Opt-in: MoreCulling's leaves mixin is only cancelled when
    // "cancel_moreculling_leaves": true is set in config/seleafculling.json.
    // Cancelling it disables all MoreCulling leaf culling modes (Gap, Depth, ...),
    // so by default both mods are left to run together.
    private final boolean cancelMoreCullingLeaves = readFlag();

    @SuppressWarnings("deprecation") // Older Gson versions lack JsonParser.parseReader.
    private static boolean readFlag() {
        try {
            Path file = FabricLoader.getInstance().getConfigDir().resolve("seleafculling.json");
            if (!Files.exists(file)) return false;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonElement value = new JsonParser().parse(reader).getAsJsonObject().get("cancel_moreculling_leaves");
                return value != null && value.isJsonPrimitive() && value.getAsBoolean();
            }
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
        // MixinSquared supplies the fully-qualified class name. Match the
        // stable mixin class suffix so MoreCulling package moves remain safe.
        return cancelMoreCullingLeaves && mixinClassName.endsWith(".LeavesBlock_typesMixin");
    }
}
//?}
