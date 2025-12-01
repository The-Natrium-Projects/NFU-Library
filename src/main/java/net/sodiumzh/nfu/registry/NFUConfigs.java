package net.sodiumzh.nfu.registry;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class NFUConfigs
{
	protected static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static ForgeConfigSpec CONFIG;
	
	public static final ForgeConfigSpec.BooleanValue SPEC_ENABLES_SAVE_DATA_PORTER;
	public static final ForgeConfigSpec.BooleanValue SPEC_DEBUG_MODE;

	public static final ForgeConfigSpec.BooleanValue ENABLES_FLYING_SPEED_SCALING_FIX;

	static
	{
		BUILDER.push("common");
		SPEC_ENABLES_SAVE_DATA_PORTER = BUILDER.comment("If true, SaveDataLocationRedirector will take effect. Setting it false could improve the performance, "
				+ "but it may cause objects (entities, items, blocks etc.) to disappear if you're using save data from an old version.")
				.define("enablesSaveDataPorter", true);
		BUILDER.pop();
		BUILDER.push("debug");
		SPEC_DEBUG_MODE = BUILDER.comment("If true, it will enable debug actions defined in NFUDebugStatics, like debug output in the chatting box.")
				.define("debugMode", false);
		BUILDER.pop();
		BUILDER.push("fixes");
		ENABLES_FLYING_SPEED_SCALING_FIX = BUILDER.comment("Vanilla has an issue that some flying mobs' flying " +
				"speed is not multiplied by AI speed modifiers (MC-172801). Fix this issue. If some mob's flying speed goes wrong, " +
				"consider disabling this config entry.")
			.define("enablesFlyingSpeedScalingFix", true);
		BUILDER.pop();
		CONFIG = BUILDER.build();
	}
	
	public static boolean CACHED_ENABLES_SAVE_DATA_PORTER = true;
	public static boolean CACHED_DEBUG_MODE = false;
	//public static boolean CACHED_CRASHES_WHEN_ENTITY_LOAD_FAILED = false;
	public static boolean CACHED_ENABLES_FLYING_SPEED_SCALING_FIX = true;
	
	public static void refresh()
	{
		CACHED_ENABLES_SAVE_DATA_PORTER = SPEC_ENABLES_SAVE_DATA_PORTER.get();
		CACHED_DEBUG_MODE = SPEC_DEBUG_MODE.get();
		CACHED_ENABLES_FLYING_SPEED_SCALING_FIX = ENABLES_FLYING_SPEED_SCALING_FIX.get();
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
