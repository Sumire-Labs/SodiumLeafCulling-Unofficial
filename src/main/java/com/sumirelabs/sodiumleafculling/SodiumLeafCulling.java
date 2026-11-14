package com.sumirelabs.sodiumleafculling;

//? if neoforge {
/*import net.neoforged.fml.common.Mod;
//? if dual_renderer {
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
//?}
*///?} elif forge {
/*import net.minecraftforge.fml.common.Mod;
*///?}

//? if !fabric {
/*@Mod(SodiumLeafCulling.MOD_ID)
public class SodiumLeafCulling {
    public static final String MOD_ID = "seleafculling";

    //? if dual_renderer {
    public SodiumLeafCulling() {
        if (FMLEnvironment.dist.isClient()) {
            ModList mods = ModList.get();
            RendererMixinPlugin.validateRendererChoice(mods.isLoaded("sodium"), mods.isLoaded("embeddium"));
        }
    }
    //?}
}
*///?}
