package net.sodiumzh.nfu.entity.anger;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.MinecraftForge;
import net.sodiumzh.nfu.entity.component.EntityComponentAPI;
import net.sodiumzh.nfu.entity.component.EntityComponentBase;
import net.sodiumzh.nfu.entity.component.EntityComponentType;
import net.sodiumzh.nfu.registry.NFUEntityComponents;
import org.apache.commons.lang3.mutable.MutableObject;
import org.checkerframework.checker.units.qual.C;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * {@code CMobAngerHandler} is a capability handling mechanics that mob can be angry with other living entities when some
 * event happens (e.g. attack). This capability doesn't to anything other than keeping an anger list.
 */
public class MobAngerHandlerComponent extends EntityComponentBase<Mob> {

    private MobAngerRules rules;
    protected final Map<UUID, Integer> angerList = new HashMap<>();
    // Just for preventing tick() from repeatedly creating sets
    private final Set<UUID> tempRemoval = new HashSet<>();
    private float damageThreshold = 1e-3f;

    public MobAngerHandlerComponent(Mob mob, MobAngerRules rules) {
        super(mob);
        this.rules = rules;
    }

    public MobAngerHandlerComponent(Mob mob) {
        this(mob, MobAngerRules.ATTACKER.get());
    }

    @Override
    public void tick() {
        angerList.entrySet().stream().toList().forEach(entry -> {
            if (entry.getValue() <= 0)
                this.tempRemoval.add(entry.getKey());
            else angerList.put(entry.getKey(), entry.getValue() - 1);
        });
        this.tempRemoval.forEach(k -> {
            angerList.remove(k);
            this.onForgive(k, new MobForgiveResult(k, true, false));
        });
        this.tempRemoval.clear();
    }

    public final MobAngerRules getAngerRules() {
        return rules;
    }

    public void setAngerRules(@Nonnull MobAngerRules rules) {
        this.rules = rules;
    }

    public final boolean isAngryAt(LivingEntity target) {
        return angerList.containsKey(target.getUUID());
    }

    /**
     * Set the mob angry with a target with no reason.
     * <p>Note: this bypasses the anger rules. Prioritize using
     * {@link MobAngerHandlerComponent#setAngryAt(LivingEntity, MobAngerReason)} instead.
     */
    public MobSetAngerResult setAngryAt(LivingEntity target, @Nullable MobAngerReason reason, int forgivingTicks) {
        if (forgivingTicks == 0)
            return MobSetAngerResult.unhandled(target, reason);
        var event = new MobAngryAtEvent(this.getEntity(), target, reason, forgivingTicks);
        if (MinecraftForge.EVENT_BUS.post(event))
            return MobSetAngerResult.unhandled(target, reason);
        if (event.getForgivingTime() == 0)
            return MobSetAngerResult.unhandled(target, reason);
        if (setAngryInternal(target, forgivingTicks))
        {
            MobSetAngerResult res = MobSetAngerResult.handled(target, reason);
            this.onAngryAt(target, forgivingTicks, res);
            return res;
        }
        else return MobSetAngerResult.unhandled(target, reason);
    }

    /**
     * Set the mob angry with a target, according to the anger rules.
     */
    public MobSetAngerResult setAngryAt(LivingEntity target, MobAngerReason reason) {
        return this.setAngryAt(target, reason, this.rules.getForgivingTicks(reason, this.getEntity(), target));
    }

    /**
     * Set the mob angry with a target with no reason.
     * <p>Note: this bypasses the anger rules. Prioritize using
     * {@link MobAngerHandlerComponent#setAngryAt(LivingEntity, MobAngerReason)} instead.
     */
    public final MobSetAngerResult setAngryAt(LivingEntity target, int forgivingTicks) {
        if (setAngryInternal(target, forgivingTicks)) {
            MobSetAngerResult res = new MobSetAngerResult(target.getUUID(), true, Optional.empty());
            this.onAngryAt(target, forgivingTicks, res);
            return res;
        }
        return new MobSetAngerResult(target.getUUID(), false, Optional.empty());
    }

    private boolean setAngryInternal(LivingEntity target, int forgivingTicks) {
        if (forgivingTicks == 0) return false;
        if (!angerList.containsKey(target.getUUID())) {
            angerList.put(target.getUUID(), forgivingTicks);
            return true;
        } else if (forgivingTicks < 0 || forgivingTicks > angerList.get(target.getUUID())) {
            angerList.put(target.getUUID(), forgivingTicks);
            return true;
        }
        return false;
    }

    public final int getRemainingForgivingTicks(LivingEntity target) {
        return isAngryAt(target) ? angerList.get(target.getUUID()) : 0;
    }

    public final MobForgiveResult forgive(LivingEntity target) {
        if (this.angerList.containsKey(target.getUUID())) {
            this.angerList.remove(target.getUUID());
            MobForgiveResult res = new MobForgiveResult(target.getUUID(), true, true);
            this.onForgive(target.getUUID(), res);
        }
        return new MobForgiveResult(target.getUUID(), false, true);
    }

    public float getDamageThreshold() {
        return this.damageThreshold;
    }

    public void setDamageThreshold(float value) {
        this.damageThreshold = value;
    }

    public CompoundTag saveAngerList() {
        CompoundTag nbt = new CompoundTag();
        for (var e : angerList.entrySet()) {
            nbt.put(e.getKey().toString(), IntTag.valueOf(e.getValue()));
        }
        return nbt;
    }

    public void loadAngerList(CompoundTag nbt) {
        angerList.clear();
        for (var key : nbt.getAllKeys()) {
            angerList.put(UUID.fromString(key), nbt.getInt(key));
        }
    }

    public void onAngryAt(LivingEntity target, int forgivingTicks, MobSetAngerResult setResult) {

    }

    public void onForgive(UUID target, MobForgiveResult setResult) {

    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("angerList", this.saveAngerList());
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.loadAngerList(nbt.getCompound("angerList"));
    }

    public static List<MobAngerHandlerComponent> getAllAngerHandlers(Entity e) {
       return EntityComponentAPI.getComponentManager(e).getDownstreamComponents().stream()
                .filter(c -> c instanceof MobAngerHandlerComponent)
                .map(c -> (MobAngerHandlerComponent)c)
                .toList();
    }

    /**
     * Set angry at a target for a mob in all its anger handlers.
     */
    public static void setAngryAtForMob(Mob mob, LivingEntity target, MobAngerReason reason) {
        EntityComponentAPI.getComponentManager(mob).getDownstreamComponents().stream()
                .filter(c -> c instanceof MobAngerHandlerComponent)
                .map(c -> (MobAngerHandlerComponent)c)
                .forEach(c -> c.setAngryAt(target, reason));
    }

}
