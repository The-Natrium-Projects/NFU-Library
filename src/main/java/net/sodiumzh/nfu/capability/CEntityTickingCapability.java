package net.sodiumzh.nfu.capability;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * CEntityTickingCapability is the base interface for entity capabilities that are automatically ticked every entity tick.
 * </p>
 *
 * <p>
 * Usage:
 * <ul>
 *   <li>Implement this interface in your capability.</li>
 *   <li>Register your capability for ticking by calling {@link #registerTicking(Capability)} (usually in a static block).</li>
 *   <li>Override {@link #tick()} to define your tick logic. This will be called every tick on every entity with the capability present and on the side given by {@link #getTickingSide()} (default SERVER).</li>
 * </ul>
 * </p>
 *
 * <p>
 * All registered ticking capabilities are automatically ticked for each entity by a global event listener that responds to EntityTickEvent (inserted via Mixin). If the capability also implements {@link CEntityTimerCapability}, {@code tickTimer()} is also called every tick on that capability.
 * </p>
 *
 * <p>
 * Ticking occurs on the logical side(s) specified by {@link #getTickingSide()}. This ticking is not affected by Forge's LivingTickEvent or LivingUpdateEvent.
 * </p>
 *
 * @param <T> The entity type this capability attaches to.
 */
public interface CEntityTickingCapability<T extends Entity>
{
	static final Set<Capability<? extends CEntityTickingCapability<? extends Entity>>> ALL_CAPS = new HashSet<>();
	public void tick();
	public T getEntity();

	/**
	 * Get on which side(s) it should tick. Server by default.
	 * <p>Be careful to use {@code BOTH}, as it will tick independently on each side, and will not sync by default.</p>
	 */
	public default TickingSide getTickingSide()
	{
		return TickingSide.SERVER;
	}
	/**
	 * Register a capability as ticking, so that it can be auto ticked.
	 */
	public static void registerTicking(Capability<? extends CEntityTickingCapability<? extends Entity>> cap)
	{
		ALL_CAPS.add(cap);
	}

	public static enum TickingSide {
		SERVER,
		CLIENT,
		BOTH;

		public boolean isCorrectSide(boolean isClientSide)
		{
			if (isClientSide)
				return this == CLIENT || this == BOTH;
			else return this == SERVER || this == BOTH;
		}
	}
}