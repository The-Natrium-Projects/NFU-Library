package net.sodiumzh.nfu.compat.jer;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IAdvancedRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.sodiumzh.nfu.NFULibrary;

@JeiPlugin
public class NFUJERPlugin implements IModPlugin
{
    @Override
    public ResourceLocation getPluginUid()
    {
        return new ResourceLocation(NFULibrary.MOD_ID, "jer_loot");
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration)
    {
        // All category plugins have run, but no recipes have been indexed yet.
        if (ModList.get().isLoaded("jeresources"))
            JERLootCompat.update();
    }
}
