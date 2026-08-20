package net.sodiumzh.nfu.entity.vanillatrade;

import net.neoforged.eventbus.api.Event;

public class VanillaTradeRegistryEvent extends Event {

    private final VanillaTradeRegistry registry;

    public VanillaTradeRegistryEvent(VanillaTradeRegistry registry) { this.registry = registry; }
}
