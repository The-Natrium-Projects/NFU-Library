package net.sodiumzh.nfu.entity.component;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.sodiumzh.nfu.capability.CEntityTickingCapability;

public class EntityComponentStatics {

    public static void init(){}

    // Use EntityComponentAPI#getComponentManager to access.
    static Capability<CEntityComponentManager> CAP_MANAGER = CapabilityManager.get(new CapabilityToken<>(){});

    static {
        CEntityTickingCapability.registerTicking(CAP_MANAGER);
    }



}
