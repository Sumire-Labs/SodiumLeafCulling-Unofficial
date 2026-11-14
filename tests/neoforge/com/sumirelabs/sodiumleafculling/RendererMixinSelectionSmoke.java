package com.sumirelabs.sodiumleafculling;

import java.util.HashSet;
import java.util.List;
import java.util.jar.JarFile;

/** Runs without a game window against the compiled NeoForge compatibility plugin. */
public final class RendererMixinSelectionSmoke {
    public static void main(String[] args) throws Exception {
        RendererMixinPlugin.validateRendererChoice(true, false);
        RendererMixinPlugin.validateRendererChoice(false, true);
        for (boolean installed : new boolean[]{false, true}) {
            try {
                RendererMixinPlugin.validateRendererChoice(installed, installed);
                throw new AssertionError("An absent or conflicting renderer must be rejected");
            } catch (IllegalStateException expected) {
                if (expected.getMessage() == null || expected.getMessage().isEmpty())
                    throw new AssertionError("Renderer error must explain the requirement");
            }
        }
        if (args.length != 1) throw new AssertionError("Packaged compatibility JAR is required");
        try (JarFile jar = new JarFile(args[0])) {
            for (boolean embeddium : new boolean[]{false, true}) {
                List<String> selected = RendererMixinPlugin.rendererMixins(embeddium);
                if (selected.size() != new HashSet<>(selected).size()) throw new AssertionError("Duplicate mixins");
                if (selected.contains("AbstractBlockRenderContextAccessor") == embeddium)
                    throw new AssertionError("Sodium's required accessor must only be selected for Sodium");
                String root = "com/sumirelabs/sodiumleafculling/" + (embeddium ? "embeddium/" : "") + "mixins/";
                for (String mixin : selected) {
                    if (jar.getJarEntry(root + mixin + ".class") == null)
                        throw new AssertionError("Selected mixin is missing from the runtime JAR: " + root + mixin);
                }
            }
        }
        System.out.println("Renderer Mixin selection passed: Sodium accessor registered; Embeddium unaffected");
    }
}
