package net.sodiumzh.nfu.entity.component;

import net.neoforged.common.capabilities.Capability;
import net.neoforged.common.capabilities.CapabilityManager;
import net.neoforged.common.capabilities.CapabilityToken;
import net.sodiumzh.nfu.capability.CEntityTickingCapability;

public class EntityComponentStatics {

    public static void init(){}

    // Use EntityComponentAPI#getComponentManager to access.
    static Capability<CEntityComponentManager> CAP_MANAGER = CapabilityManager.get(new CapabilityToken<>(){});

    static {
        CEntityTickingCapability.registerTicking(CAP_MANAGER);
    }



}
