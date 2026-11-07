package com.sumirelabs.sodiumleafculling;

import java.util.HashSet;
import java.util.List;

/** Runs without a game window against the compiled NeoForge compatibility plugin. */
public final class RendererMixinSelectionSmoke {
    public static void main(String[] args) {
        for (boolean embeddium : new boolean[]{false, true}) {
            List<String> selected = RendererMixinPlugin.rendererMixins(embeddium);
            if (selected.size() != new HashSet<>(selected).size()) throw new AssertionError("Duplicate mixins");
            if (selected.contains("AbstractBlockRenderContextAccessor") == embeddium)
                throw new AssertionError("Sodium's required accessor must only be selected for Sodium");
            String root = "com/sumirelabs/sodiumleafculling/" + (embeddium ? "embeddium/" : "") + "mixins/";
            // The Embeddium classes are packaged from the auxiliary project rather than
            // the development classpath. Validate its expected list here, and Sodium's
            // complete list of actual class resources.
            if (!embeddium) {
                for (String mixin : selected) {
                    if (RendererMixinSelectionSmoke.class.getClassLoader().getResource(root + mixin + ".class") == null)
                        throw new AssertionError("Selected mixin is missing: " + mixin);
                }
            }
        }
        System.out.println("Renderer Mixin selection passed: Sodium accessor registered; Embeddium unaffected");
    }
}
