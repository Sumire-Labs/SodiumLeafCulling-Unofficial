import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/** Runs inside Fabric's real Mixin environment before starting the graphical client. */
public final class ConfigRegistrationSmoke implements PreLaunchEntrypoint {
    private static final ClassLoader LOADER = Thread.currentThread().getContextClassLoader();

    @Override
    public void onPreLaunch() {
        try {
            persistence();
            if (present("net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder")) {
                modern();
            } else {
                legacy();
            }
            if (FabricLoader.getInstance().getModContainer("minecraft").get()
                    .getMetadata().getVersion().getFriendlyString().equals("1.21.1")) adjacentFaces();
            System.out.println("SLC_CONFIG_SMOKE_PASSED: registration, defaults, and live slider availability");
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void modern() throws Exception {
        String root = "net.caffeinemc.mods.sodium.";
        Class<?> targetClass = type(root + "client.gui.SodiumConfigBuilder");
        Object target = allocate(targetClass);
        Object builder = type(root + "client.config.builder.ConfigBuilderImpl")
                .getConstructor(Function.class, String.class).newInstance((Function<String, Object>) id -> null, "smoke");
        injected(targetClass).invoke(target, builder, callback());
        List<?> registered = (List<?>) field(builder.getClass(), "pendingModConfigBuilders").get(builder);
        check(registered.size() == 1, "Leaf Culling category registered separately");
        check(field(registered.get(0).getClass(), "name").get(registered.get(0)).equals("Leaf Culling"),
                "category is named Leaf Culling");
        Object page = declaredPage(targetClass).invoke(null, builder);
        List<?> groups = (List<?>) field(page.getClass(), "groups").get(page);
        List<?> optionsList = (List<?>) call(groups.get(0), "options");
        check(optionsList.size() == 4, "four options registered");
        Object toggle = optionsList.get(1), slider = optionsList.get(2);
        check(call(slider, "formatValue", 64) != null, "distance formatter is configured");
        Object enabled = call(slider, "getEnabled");
        @SuppressWarnings("unchecked")
        Function<Object, Object> provider = (Function<Object, Object>) field(enabled.getClass(), "provider").get(enabled);
        Class<?> state = type(root + "api.config.ConfigState");
        for (boolean value : new boolean[]{false, true, false}) {
            Object mockState = Proxy.newProxyInstance(LOADER, new Class<?>[]{state}, (p, m, a) -> {
                check(m.getName().equals("readBooleanOption"), "only the toggle is read");
                check(a[0].toString().equals("sodiumleafculling:performance.distance_leaf_culling"), "correct toggle dependency");
                return value;
            });
            check(provider.apply(mockState).equals(value), "live modern availability");
        }
        check(!((Collection<?>) call(enabled, "getDependencies")).isEmpty(), "availability dependency registered");
        System.out.println("SLC_CONFIG_SMOKE_MODERN: " + toggle.getClass().getName());
    }

    private static void legacy() throws Exception {
        String root = present("net.caffeinemc.mods.sodium.client.gui.SodiumGameOptionPages")
                ? "net.caffeinemc.mods.sodium.client." : "me.jellysquid.mods.sodium.client.";
        Object settings = callStatic(type("com.sumirelabs.sodiumleafculling.LeafCullingConfig"), "get");
        defaults(settings);
        // Later legacy pages cache getInstance().getWindow() in their static initializer.
        // A constructor-free singleton lets them cache null without creating a native window.
        Class<?> minecraft = type("net.minecraft.client.Minecraft");
        for (Field singleton : minecraft.getDeclaredFields()) {
            if (Modifier.isStatic(singleton.getModifiers()) && singleton.getType() == minecraft) {
                singleton.setAccessible(true);
                if (singleton.get(null) == null) singleton.set(null, allocate(minecraft));
            }
        }
        Class<?> gui = type(root + "gui.SodiumOptionsGUI");
        Object screen = allocate(gui);
        Field pages = field(gui, "pages");
        pages.set(screen, Collections.emptyList());
        injected(gui).invoke(screen, callback());
        List<?> pageList = (List<?>) pages.get(screen);
        check(pageList.size() == 1, "one separate legacy page registered");
        List<?> optionsList = (List<?>) call(pageList.get(0), "getOptions");
        check(optionsList.size() == 4, "four legacy options registered");
        Object toggle = optionsList.get(1), slider = optionsList.get(2);
        Object controlOption = call(call(slider, "getControl"), "getOption");
        call(toggle, "setValue", false);
        check(call(controlOption, "isAvailable").equals(false), "slider initially disabled");
        call(controlOption, "setValue", 80);
        check(call(slider, "getValue").equals(64), "disabled slider rejects edits");
        call(toggle, "setValue", true);
        check(call(controlOption, "isAvailable").equals(true), "slider enabled before applying");
        call(controlOption, "setValue", 80);
        check(call(slider, "getValue").equals(80), "enabled slider accepts edits");
        call(toggle, "setValue", false);
        check(call(controlOption, "isAvailable").equals(false), "slider disabled before applying");
        check(call(slider, "getValue").equals(80), "distance retained when disabled");
        check(call(settings, "sodiumleafculling$getDistance").equals(64), "unapplied settings preserved");
        System.out.println("SLC_CONFIG_SMOKE_LEGACY: " + root);
    }

    private static void defaults(Object settings) throws Exception {
        check(call(settings, "sodiumleafculling$getQuality").toString().equals("SOLID"), "default mode is Solid");
        check(call(settings, "sodiumleafculling$isDistanceCullingEnabled").equals(false), "distance default off");
        check(call(settings, "sodiumleafculling$getDistance").equals(64), "default distance 64");
    }

    private static void persistence() throws Exception {
        Class<?> config = type("com.sumirelabs.sodiumleafculling.LeafCullingConfig");
        Constructor<?> ctor = config.getDeclaredConstructor(Path.class);
        ctor.setAccessible(true);
        Path dir = Files.createTempDirectory("seleafculling-config-smoke-");
        Path source = dir.resolve("sodium-options.json");
        Path independent = dir.resolve("seleafculling.cfg");
        byte[] original = ("{\"performance\":{\"leaf_culling_quality\":\"HOLLOW\"," +
                "\"distance_leaf_culling\":true,\"leaf_culling_distance\":91}}").getBytes(StandardCharsets.UTF_8);
        try {
            Files.write(source, original);
            Object migrated = ctor.newInstance(dir);
            check(call(migrated, "sodiumleafculling$getQuality").toString().equals("HOLLOW"), "old mode migrated");
            check(call(migrated, "sodiumleafculling$isDistanceCullingEnabled").equals(true), "old toggle migrated");
            check(call(migrated, "sodiumleafculling$getDistance").equals(91), "old distance migrated");
            check(call(migrated, "cullAdjacentBlocks").equals(false), "new optimization defaults off");
            call(migrated, "sodiumleafculling$setDistance", 42);
            call(migrated, "setCullAdjacentBlocks", true);
            call(migrated, "save");
            Object loaded = ctor.newInstance(dir);
            check(call(loaded, "sodiumleafculling$getDistance").equals(42), "own file takes precedence");
            check(call(loaded, "cullAdjacentBlocks").equals(true), "new optimization persists");
            check(Arrays.equals(Files.readAllBytes(source), original), "renderer settings file preserved");
        } finally {
            Files.deleteIfExists(independent);
            Files.deleteIfExists(source);
            Files.deleteIfExists(dir);
        }
    }

    /** Count faces with the actual culling predicate and Minecraft block states. */
    private static void adjacentFaces() throws Exception {
        callStatic(type("net.minecraft.SharedConstants"), "tryDetectVersion");
        callStatic(type("net.minecraft.server.Bootstrap"), "bootStrap");
        Class<?> blocks = type("net.minecraft.world.level.block.Blocks");
        Object log = call(blocks.getField("OAK_LOG").get(null), "defaultBlockState");
        Object leaf = call(blocks.getField("OAK_LEAVES").get(null), "defaultBlockState");
        Object air = call(blocks.getField("AIR").get(null), "defaultBlockState");
        Object glass = call(blocks.getField("GLASS").get(null), "defaultBlockState");
        Class<?> pos = type("net.minecraft.core.BlockPos");
        Constructor<?> makePos = pos.getConstructor(int.class, int.class, int.class);
        Class<?> getter = type("net.minecraft.world.level.BlockGetter");
        Object view = Proxy.newProxyInstance(LOADER, new Class<?>[]{getter}, (p, m, a) -> {
            check(m.getName().equals("getBlockState"), "adjacent check only needs block states");
            int x = (int) call(a[0], "getX"), y = (int) call(a[0], "getY"), z = (int) call(a[0], "getZ");
            if (x == 8 && z == 8 && y >= 5 && y <= 7) return log;
            return x >= 6 && x <= 10 && y >= 3 && y <= 9 && z >= 6 && z <= 10 ? leaf : air;
        });
        Class<?> culling = type("com.sumirelabs.sodiumleafculling.LeafCulling");
        Object solid = type("com.sumirelabs.sodiumleafculling.LeafCullingQuality").getField("SOLID").get(null);
        Object center = makePos.newInstance(8, 6, 8);
        Object exposed = Proxy.newProxyInstance(LOADER, new Class<?>[]{getter}, (p, m, a) -> air);
        int count = 0, culled = 0;
        callStatic(culling, "beginOcclusionBuild");
        try {
            check(!(boolean) callStatic(culling, "shouldCullAdjacentBlockFace", view, center, log, 9, 6, 8),
                    "an uncached neighbor does not trigger a scan");
            for (int y = 5; y <= 7; y++) {
                Object logPos = makePos.newInstance(8, y, 8);
                for (int[] d : new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}) {
                    int x = 8 + d[0], ny = y + d[1], z = 8 + d[2];
                    Object next = makePos.newInstance(x, ny, z);
                    Object neighbor = call(view, "getBlockState", next);
                    if (neighbor == log) continue; // Vanilla hides internal log-to-log faces.
                    count++;
                    callStatic(culling, "surroundedByLeaves", view, next, solid);
                    if ((boolean) callStatic(culling, "shouldCullAdjacentBlockFace", view, logPos, log, x, ny, z))
                        culled++;
                }
            }
            check(count == 14 && culled == 14, "all preclassified faces are hidden");
            check(!(boolean) callStatic(culling, "shouldCullAdjacentBlockFace", view, center, glass, 9, 6, 8),
                    "translucent blocks are not culled");
            Object boundary = makePos.newInstance(15, 6, 8);
            callStatic(culling, "surroundedByLeaves", view, boundary, solid);
            check(!(boolean) callStatic(culling, "shouldCullAdjacentBlockFace", view,
                    makePos.newInstance(14, 6, 8), log, 15, 6, 8), "section boundary is preserved");
            check(!(boolean) callStatic(culling, "shouldCullAdjacentBlockFace", exposed, center, log, 9, 6, 8),
                    "another world view cannot supply a cached occluder");
        } finally {
            callStatic(culling, "endOcclusionBuild");
        }
        System.out.println("SLC_ADJACENT_FACES: " + count + " -> " + (count - culled) + " log faces with preclassified leaves");
        int[] lookups = {0};
        boolean[] enclosed = {true};
        Object countedView = Proxy.newProxyInstance(LOADER, new Class<?>[]{getter}, (p, m, a) -> {
            lookups[0]++;
            return enclosed[0] ? leaf : air;
        });
        callStatic(culling, "beginOcclusionBuild");
        try {
            for (int i = 0; i < 8; i++) {
                check((boolean) callStatic(culling, "surroundedByLeaves", countedView, center, solid),
                        "cached enclosure equals original");
            }
            check(lookups[0] == 6, "eight queries read the six neighbors only once");
            check(!(boolean) callStatic(culling, "surroundedByLeaves", exposed, center, solid),
                    "different views do not reuse enclosure entries");
        } finally {
            callStatic(culling, "endOcclusionBuild");
        }
        enclosed[0] = false;
        callStatic(culling, "beginOcclusionBuild");
        try {
            check(!(boolean) callStatic(culling, "surroundedByLeaves", countedView, center, solid),
                    "a new mesh reads updated neighbors even when the view object is reused");
        } finally {
            callStatic(culling, "endOcclusionBuild");
        }
        System.out.println("SLC_OCCLUSION_CACHE: 48 -> 6 neighbor reads for eight enclosure queries");
    }

    private static Method injected(Class<?> target) {
        return methodContaining(target, "inject$leafcullingoption");
    }

    private static Method declaredPage(Class<?> target) {
        for (Method method : target.getDeclaredMethods()) {
            if (method.getName().contains("sodiumleafculling$createPage") &&
                    !method.getName().contains("lambda$")) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new AssertionError("Leaf Culling page builder was not applied");
    }

    private static Method methodContaining(Class<?> target, String name) {
        for (Method method : target.getDeclaredMethods()) {
            if (method.getName().contains(name)) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new AssertionError("Options Mixin was not applied to " + target.getName());
    }

    private static Object callback() throws Exception {
        return type("org.spongepowered.asm.mixin.injection.callback.CallbackInfo")
                .getConstructor(String.class, boolean.class).newInstance("smoke", false);
    }

    private static Object allocate(Class<?> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field singleton = field(unsafeClass, "theUnsafe");
        return unsafeClass.getMethod("allocateInstance", Class.class).invoke(singleton.get(null), type);
    }

    private static Object construct(Class<?> type) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static Object callStatic(Class<?> type, String name, Object... args) throws Exception {
        return invoke(type, null, name, args);
    }

    private static Object call(Object target, String name, Object... args) throws Exception {
        return invoke(target.getClass(), target, name, args);
    }

    private static Object invoke(Class<?> type, Object target, String name, Object... args) throws Exception {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                method.setAccessible(true);
                return method.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name, true, LOADER); }
    private static boolean present(String name) {
        try { Class.forName(name, false, LOADER); return true; } catch (ClassNotFoundException absent) { return false; }
    }
    private static boolean presentMethod(Class<?> type, String name) {
        try { type.getMethod(name); return true; } catch (NoSuchMethodException absent) { return false; }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
