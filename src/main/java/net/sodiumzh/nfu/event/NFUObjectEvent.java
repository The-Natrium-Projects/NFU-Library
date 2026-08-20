package net.sodiumzh.nfu.event;

import net.neoforged.eventbus.api.Event;

/**
 * An utility class of a simple event with a specified object.
 */
public abstract class NFUObjectEvent<T> extends Event
{
	protected final T object;
	
	public NFUObjectEvent(T obj)
	{
		this.object = obj;
	}
	
	public T getObject()
	{
		return object;
	}
}
