package net.sodiumzh.nfu.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A {@code RepeatableAttributeModifier} represents an attribute modifier which can be applied multiple times.
 * It's recommended to create a static instance for each {@code RepeatableAttributeModifier} in each class.
 * <p>Note: The complexity of the {@code apply} operation is O(times) because it must iterate through the whole modifier list
 * to find which modifier it's previously applying. Take care if you need to update on tick.
 * <p>Note: {@code AttributeModifier}s added by this object is always transient. The actual modifier uuids inside this object
 * are random, permanent attribute modifiers will cause duplicate applying on restarting the game.
 */
public class RepeatableAttributeModifier
{
	protected double value;
	protected final ResourceLocation name;
	protected final AttributeModifier.Operation operation;
	protected final ArrayList<AttributeModifier> modifiers = new ArrayList<>();
	/** If the modifier count is larger than this value, it will throw an exception.
	 * This limitation is to prevent the ArrayList from getting too large because it will auto-expand and generate 
	 * more {@code AttributeModifier} instances when accessing the index larger than its size.
	 */
	protected int maxSize;

	public RepeatableAttributeModifier(double value, ResourceLocation name, AttributeModifier.Operation operation, int maxRepeatTimes)
	{
		this.value = value;
		this.name = name;
		this.operation = operation;
		this.maxSize = maxRepeatTimes;
	}
	
	public RepeatableAttributeModifier(double value, ResourceLocation name, AttributeModifier.Operation operation)
	{
		this(value, name, operation, 100000);
	}
	
	public AttributeModifier get(int index)
	{
		if (index > this.maxSize)
			throw new IllegalArgumentException("Index is larger than the set max. Attempted index: " + Integer.toString(index) + "; Set max: " + Integer.toString(maxSize));
		// The first element (index == 0) is zero, so max length should be (max + 1).
		while (modifiers.size() <= index + 1)
		{
			modifiers.add(new AttributeModifier(UUID.randomUUID(), this.name.toString() + "_" + modifiers.size(), this.value * modifiers.size(), this.operation));
		}
		return modifiers.get(index);
	}
	
	public void apply(LivingEntity target, Attribute attribute, int times)
	{
		AttributeInstance inst = target.getAttribute(attribute);
		// Check if it's already applying the same modifier. This could prevent iteration on tick.
		if (inst.hasModifier(this.get(times))) {
			// If value is updated, still refresh the attribute
			UUID modifierID = this.get(times).getId();
			if (Math.abs(inst.getModifier(modifierID).getAmount() - this.get(times).getAmount()) > 1e-12) {
				inst.removeModifier(modifierID);
				inst.addTransientModifier(this.get(times));
			}
			return;
		}
		for (var modifier: modifiers)
		{
			inst.removeModifier(modifier);
		}
		inst.addTransientModifier(this.get(times));
	}

	public void clear(LivingEntity target, Attribute attribute)
	{
		AttributeInstance inst = target.getAttribute(attribute);
		for (var modifier: modifiers)
		{
			inst.removeModifier(modifier);
		}
	}
	
	/**
	 * Pre-allocate a size (usually on construction) to prevent jam on first applying.
	 */
	public RepeatableAttributeModifier initSize(int size)
	{
		this.get(size);
		return this;
	}

	/**
	 * Reset the amount. Note that entities that already applied the attributes must run {@code apply} again to update the values.
	 */
	public void resetAmount(double value) {
		List<AttributeModifier> newModifiers = this.modifiers.stream()
			.map(am -> new AttributeModifier(am.getId(), am.getName(), value, am.getOperation()))
			.toList();
		for (int i = 0; i < this.modifiers.size(); ++i) {
			this.modifiers.set(i, newModifiers.get(i));
		}
		this.value = value;
	}


}
