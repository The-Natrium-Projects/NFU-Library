package net.sodiumzh.nfu.registry;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class NFUConfigs
{
	protected static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static ForgeConfigSpec CONFIG;
	
	public static final ForgeConfigSpec.BooleanValue SPEC_ENABLES_SAVE_DATA_PORTER;
	public static final ForgeConfigSpec.BooleanValue SPEC_DEBUG_MSG_OUTPUT;
	public static final ForgeConfigSpec.BooleanValue SPEC_ENTITY_COMPONENT_HIERARCHY_CHECK;
    public static final ForgeConfigSpec.BooleanValue SPEC_CRASHES_ON_ENTITY_LOAD_FAILS;

	public static final ForgeConfigSpec.BooleanValue SPEC_ENABLES_FLYING_SPEED_SCALING_FIX;

	public static final ForgeConfigSpec.IntValue SPEC_ENTITY_SYNCHER_FREQUENCY;

	static
	{
		BUILDER.push("common");
		SPEC_ENABLES_SAVE_DATA_PORTER = BUILDER.comment("If true, SaveDataLocationRedirector will take effect. Setting it false could improve the performance, "
				+ "but it may cause game objects (entities, items, blocks etc.) to disappear if you're using save data from an old version.")
				.define("enablesSaveDataPorter", true);
		SPEC_ENTITY_SYNCHER_FREQUENCY = BUILDER.comment("Amount of ticks each entity synchronization action of NFU Entity Synchers. "
				+ "Increasing this value (reducing the frequency) may improve the performance, but might cause synchronization issues.")
				.defineInRange("entitySyncherFrequency", 5, 1, 100);
		BUILDER.pop();
		BUILDER.push("fixes");
		SPEC_ENABLES_FLYING_SPEED_SCALING_FIX = BUILDER.comment("Fix of a vanilla issue that some flying mobs' flying " +
				"speed is not multiplied by AI speed modifiers (MC-172801). If some mob's flying speed goes wrong, " +
				"consider disabling this config entry.")
			.define("enablesFlyingSpeedScalingFix", true);
		BUILDER.pop();
		BUILDER.push("debug");
		BUILDER.comment("Debug options. Only enable them when you're debugging or testing mods or modpacks." +
			"These options will enable extra output or checks, and may cause lags or verbose messages.");
		SPEC_DEBUG_MSG_OUTPUT = BUILDER.comment("If true, it will enable debug actions defined in NFUDebugStatics, like debug output in the chatting box.")
				.define("debugMessageOutput", false);
		SPEC_ENTITY_COMPONENT_HIERARCHY_CHECK = BUILDER.comment("If true, the NFU Entity Component system will check hierarchy validity in runtime. Setting this " +
				"true will throw exception if the hierarchy is wrong, but will cause extra resource cost. Recommended to open only when in a debug environment.")
				.define("entityComponentHierarchyCheck", false);
        SPEC_CRASHES_ON_ENTITY_LOAD_FAILS = BUILDER.comment("If true, when an exception is thrown on entity loading from " +
            "save data, the game will crash instead of skipping the entity.")
                .define("crashesOnEntityLoadFails", false);
		BUILDER.pop();

		CONFIG = BUILDER.build();
	}
	
	public static boolean CACHED_ENABLES_SAVE_DATA_PORTER = true;
	public static int CACHED_ENTITY_SYNCHER_FREQUENCY = 5;
	public static boolean CACHED_DEBUG_MSG_OUTPUT = false;
	public static boolean CACHED_ENTITY_COMPONENT_HIERARCHY_CHECK = false;
	public static boolean CACHED_CRASHES_ON_ENTITY_LOAD_FAILS = false;
	public static boolean CACHED_ENABLES_FLYING_SPEED_SCALING_FIX = true;
	
	public static void refresh()
	{
		CACHED_ENABLES_SAVE_DATA_PORTER = SPEC_ENABLES_SAVE_DATA_PORTER.get();
		CACHED_ENTITY_SYNCHER_FREQUENCY = SPEC_ENTITY_SYNCHER_FREQUENCY.get();
		CACHED_DEBUG_MSG_OUTPUT = SPEC_DEBUG_MSG_OUTPUT.get();
		CACHED_ENTITY_COMPONENT_HIERARCHY_CHECK = SPEC_ENTITY_COMPONENT_HIERARCHY_CHECK.get();
        CACHED_CRASHES_ON_ENTITY_LOAD_FAILS = SPEC_CRASHES_ON_ENTITY_LOAD_FAILS.get();
		CACHED_ENABLES_FLYING_SPEED_SCALING_FIX = SPEC_ENABLES_FLYING_SPEED_SCALING_FIX.get();
	}
	
	@SubscribeEvent
	public static void loadConfig(final ModConfigEvent event)
	{
		if (event.getConfig().getSpec() == CONFIG)
		{
			refresh();
		}
	}
}
