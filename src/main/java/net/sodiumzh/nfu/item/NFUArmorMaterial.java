package net.sodiumzh.nfu.item;

import com.google.common.base.Supplier;
import com.google.common.collect.BiMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.LazyLoadedValue;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.crafting.Ingredient;
import net.sodiumzh.nfu.util.NFUResourceLocation;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class NFUArmorMaterial implements ArmorMaterial {

    protected static final ArmorItem.Type[] TYPE_BY_INDEX = new ArmorItem.Type[]{
        ArmorItem.Type.BOOTS, ArmorItem.Type.LEGGINGS, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.HELMET
    };
    protected static final Map<ArmorItem.Type, Integer> TYPE_TO_INDEX =
        Map.of(ArmorItem.Type.BOOTS, 0, ArmorItem.Type.LEGGINGS, 1, ArmorItem.Type.CHESTPLATE, 2, ArmorItem.Type.HELMET, 3);
    protected final int[] durabilities;
    protected ResourceLocation name;
    protected final int[] slotProtections;
    protected final int enchantmentValue;
    protected SoundEvent sound;
    protected final float toughness;
    protected final float knockbackResistance;
    protected final LazyLoadedValue<Ingredient> repairIngredient;

    public NFUArmorMaterial(ResourceLocation pName, int[] durabilities, int[] pSlotProtections, int pEnchantmentValue,
                            SoundEvent pSound, float pToughness, float pKnockbackResistance, Supplier<Ingredient> pRepairIngredient)
    {
        this.name = pName;
        this.durabilities = durabilities;
        this.slotProtections = pSlotProtections;
        this.enchantmentValue = pEnchantmentValue;
        this.sound = pSound;
        this.toughness = pToughness;
        this.knockbackResistance = pKnockbackResistance;
        this.repairIngredient = new LazyLoadedValue<>(pRepairIngredient);
    }

    /**
     * Copy an existing armor material with a new name.
     */
    public static NFUArmorMaterial copyOf(ResourceLocation newName, ArmorMaterial from) {
        return new NFUArmorMaterial(newName, durabilitiesOf(from), defensesOf(from),
            from.getEnchantmentValue(), from.getEquipSound(), from.getToughness(), from.getKnockbackResistance(), from::getRepairIngredient);
    }

    public static int[] durabilitiesOf(ArmorMaterial material) {
        return Arrays.stream(TYPE_BY_INDEX).mapToInt(material::getDurabilityForType).toArray();
    }

    public static int[] defensesOf(ArmorMaterial material) {
        return Arrays.stream(TYPE_BY_INDEX).mapToInt(material::getDefenseForType).toArray();
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return durabilities[TYPE_TO_INDEX.get(type)];
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return this.slotProtections[TYPE_TO_INDEX.get(type)];
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return this.sound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

    @Override
    public String getName() {
        return this.name.toString();
    }

    @Override
    public float getToughness() {
        return this.toughness;
    }

    /**
     * Gets the percentage of knockback resistance provided by armor of the
     * material.
     */
    @Override
    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }

    public NFUArmorMaterial setSound(SoundEvent sound) {
        this.sound = sound;
        return this;
    }
}
