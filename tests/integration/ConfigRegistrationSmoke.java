import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/** Runs inside Fabric's real Mixin environment before starting the graphical client. */
public final class ConfigRegistrationSmoke implements PreLaunchEntrypoint {
    private static final ClassLoader LOADER = Thread.currentThread().getContextClassLoader();

    @Override
    public void onPreLaunch() {
        try {
            if (present("net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder")) {
                modern();
            } else {
                legacy();
            }
            System.out.println("SLC_CONFIG_SMOKE_PASSED: registration, defaults, and live slider availability");
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void modern() throws Exception {
        String root = "net.caffeinemc.mods.sodium.";
        Class<?> optionsClass = type(root + "client.gui.SodiumOptions");
        Object options = construct(optionsClass);
        Object settings = optionsClass.getField("performance").get(options);
        defaults(settings);
        Class<?> targetClass = type(root + "client.gui.SodiumConfigBuilder");
        Object target = allocate(targetClass);
        field(targetClass, "sodiumOpts").set(target, options);
        Class<?> storageClass = type(root + "api.config.StorageEventHandler");
        Object storage = Proxy.newProxyInstance(LOADER, new Class<?>[]{storageClass}, (p, m, a) -> {
            if (m.getName().equals("hashCode")) return System.identityHashCode(p);
            if (m.getName().equals("equals")) return p == a[0];
            if (m.getName().equals("toString")) return "Smoke storage";
            return null;
        });
        field(targetClass, "sodiumStorage").set(target, storage);

        Object builder = type(root + "client.config.builder.ConfigBuilderImpl")
                .getConstructor(Function.class, String.class).newInstance((Function<String, Object>) id -> null, "smoke");
        Object page = call(builder, "createOptionPage");
        Object component = callStatic(type("net.minecraft.network.chat.Component"), "literal", "Smoke");
        call(page, "setName", component);
        Object callback = callback(page);
        injected(targetClass).invoke(target, callback, builder);

        List<?> groups = (List<?>) field(page.getClass(), "groups").get(page);
        List<?> optionsList = (List<?>) call(groups.get(0), "options");
        check(optionsList.size() == 3, "three options registered");
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
        Class<?> optionsClass = type(root + "gui.SodiumGameOptions");
        Object options = construct(optionsClass);
        Object settings;
        try {
            Object advanced = optionsClass.getField("advanced").get(options);
            if (presentMethod(advanced.getClass(), "sodiumleafculling$getDistance")) settings = advanced;
            else settings = optionsClass.getField("performance").get(options);
        } catch (NoSuchFieldException missing) {
            settings = optionsClass.getField("performance").get(options);
        }
        defaults(settings);
        field(type(root + "SodiumClientMod"), "CONFIG").set(null, options);
        // Later legacy pages cache getInstance().getWindow() in their static initializer.
        // A constructor-free singleton lets them cache null without creating a native window.
        Class<?> minecraft = type("net.minecraft.client.Minecraft");
        for (Field singleton : minecraft.getDeclaredFields()) {
            if (Modifier.isStatic(singleton.getModifiers()) && singleton.getType() == minecraft) {
                singleton.setAccessible(true);
                if (singleton.get(null) == null) singleton.set(null, allocate(minecraft));
            }
        }
        Class<?> pages = type(root + "gui.SodiumGameOptionPages");
        List<Object> groups = new ArrayList<>();
        injected(pages).invoke(null, null, groups);
        List<?> optionsList = (List<?>) call(groups.get(0), "getOptions");
        check(optionsList.size() == 3, "three legacy options registered");
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

    private static Method injected(Class<?> target) {
        for (Method method : target.getDeclaredMethods()) {
            if (method.getName().contains("inject$leafcullingoption") && method.getParameterCount() == 2 &&
                    method.getParameterTypes()[0].getSimpleName().equals("CallbackInfoReturnable")) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new AssertionError("Options Mixin was not applied to " + target.getName());
    }

    private static Object callback(Object page) throws Exception {
        return type("org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable")
                .getConstructor(String.class, boolean.class, Object.class).newInstance("smoke", false, page);
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
