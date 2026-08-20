package net.sodiumzh.nfu.item.bauble;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.neoforged.common.capabilities.Capability;
import net.neoforged.common.capabilities.ICapabilityProvider;
import net.neoforged.common.util.LazyOptional;

class CBaubleEquippableMobPrvd implements ICapabilityProvider
{
	
	private CBaubleEquippableMob cap;
	
	public CBaubleEquippableMobPrvd(Mob mob)
	{
		cap = new CBaubleEquippableMobImpl(mob);
	}
	
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if(cap == BaubleSystemCapabilities.CAP_BAUBLE_EQUIPPABLE_MOB)
			return LazyOptional.of(() -> {return this.cap;}).cast();
		else
			return LazyOptional.empty();
	}
}
