package net.sodiumzh.nfu.util;

import net.minecraft.resources.ResourceLocation;

/**
 * Resource Location helper methods to uniform 1.21.1+ and lower resource location construction methods.
 */
public class NFUResourceLocation {

    public static ResourceLocation of(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation of(String byString) {
        return new ResourceLocation(byString);
    }

}
