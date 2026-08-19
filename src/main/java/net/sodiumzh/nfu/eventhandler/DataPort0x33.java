package net.sodiumzh.nfu.eventhandler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sodiumzh.nfu.NFULibrary;
import net.sodiumzh.nfu.mixin.event.entity.EntityLoadEvent;
import org.checkerframework.checker.units.qual.C;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = NFULibrary.MOD_ID)
public class DataPort0x33 {

    @SubscribeEvent
    public static void portEntityComponents(EntityLoadEvent event) {
        CompoundTag mgr = event.getNBT().getCompound("ForgeCaps").getCompound("nfulib:entity_component_manager");
        if (mgr.isEmpty()) return;
        // Port legacy "dynamic_data" to new "data"
        if (mgr.getCompound("dynamic_data").getString("type").equals("nfulib:dynamic_data")) {
            CompoundTag data = new CompoundTag();
            data.putString("name", "data");
            data.put("subcomponents", new CompoundTag());
            data.putString("type", "nfulib:data");
            CompoundTag dataVal = new CompoundTag();
            dataVal.put("nbt", mgr.getCompound("dynamic_data").getCompound("data").copy());
            data.put("data", dataVal);
            mgr.put("data", data);
            mgr.remove("dynamic_data");
        }
        // Port legacy "default_timer" to "timer"
        CompoundTag timer = mgr.getCompound("default_timer");
        if (timer.getString("type").equals("nfulib:default_timer")) {
            timer.remove("name");
            timer.put("subcomponents", new CompoundTag());
            timer.putString("type", "nfulib:timer");
            mgr.put("timer", timer.copy());
            mgr.remove("default_timer");
        }
    }
}
