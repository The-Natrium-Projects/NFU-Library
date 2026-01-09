package net.sodiumzh.nfu.entity.component;

import javax.annotation.Nullable;

/**
 * Utility for objects (Entity, Level, Block Entity) that expose their own component manager and provide initialization logic.
 * <p>
 * When the capability is attached to such an entity for the first time (i.e. during fresh entity creation, not loading from NBT),
 * the manager initializer will be called automatically. This allows custom default component tree setup.
 * <b>Note:</b> The initializer is only invoked for entities that are newly created in the world, and will
 * <b>not</b> be called during loading/deserialization from disk or network—deserialized entities will
 * reconstruct their component tree from saved data instead.
 * </p>
 */
public interface IEntityComponentManagerHolder {

    /**
     * @return The CEntityComponentManager instance for this entity, or null if not (yet) constructed.
     */
    @Nullable
    CEntityComponentManager getComponentManager();

    /**
     * Called when the capability is attached and the manager is (about to be) available.
     *
     * <p>
     * Implementors should build and wire up their desired default component tree in this method.
     * <b>This initializer is only called for freshly created entities (not when loading from save data).</b>
     * </p>
     *
     * @param manager The manager instance about to be registered on the entity.
     */
    void initializeComponents(CEntityComponentManager manager);

}