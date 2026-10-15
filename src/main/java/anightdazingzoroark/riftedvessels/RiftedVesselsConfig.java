package anightdazingzoroark.riftedvessels;

import net.minecraftforge.common.config.Config;

@Config(modid = RiftedVessels.MOD_ID)
public class RiftedVesselsConfig {
    @Config.Name("Ship Helm Acceleration")
    @Config.Comment("Horizontal acceleration in blocks per second squared while piloting with a Ship Helm.")
    @Config.RangeDouble(min = 0.1D, max = 100D)
    public static double shipHelmAcceleration = 3D;

    @Config.Name("Ship Helm Maximum Speed")
    @Config.Comment("Maximum horizontal speed in blocks per second while piloting with a Ship Helm.")
    @Config.RangeDouble(min = 0.1D, max = 1000D)
    public static double shipHelmMaximumSpeed = 12D;

    @Config.Name("Ship Engine Speed Bonus")
    @Config.Comment("Additional maximum speed in blocks per second provided by each burning Ship Engine.")
    @Config.RangeDouble(min = 0D, max = 100D)
    public static double shipEngineSpeedBonus = 2D;

    @Config.Name("Ship Engine Maximum Speed Bonus")
    @Config.Comment("Maximum combined speed bonus in blocks per second that burning Ship Engines can provide to a ship.")
    @Config.RangeDouble(min = 0D, max = 1000D)
    public static double shipEngineMaximumSpeedBonus = 20D;

    @Config.Name("Ship Engine Acceleration")
    @Config.Comment("Acceleration in blocks per second squared provided by each burning Ship Engine.")
    @Config.RangeDouble(min = 0D, max = 100D)
    public static double shipEngineAcceleration = 1D;

    @Config.Name("Ship Helm Turn Acceleration")
    @Config.Comment("Turning acceleration in degrees per second squared while piloting with a Ship Helm.")
    @Config.RangeDouble(min = 0.1D, max = 360D)
    public static double shipHelmTurnAcceleration = 30D;

    @Config.Name("Ship Helm Maximum Turn Speed")
    @Config.Comment("Maximum turning speed in degrees per second while piloting with a Ship Helm.")
    @Config.RangeDouble(min = 0.1D, max = 360D)
    public static double shipHelmMaximumTurnSpeed = 45D;

    @Config.Name("Ship Helm Blacklist")
    @Config.Comment("Blocks to not be included when assembling a ship using the Ship Helm.")
    public static String[] shipHelmBlacklist = {
            "minecraft:air", "minecraft:dirt", "minecraft:grass", "minecraft:stone",
            "minecraft:tallgrass", "minecraft:water", "minecraft:flowing_water", "minecraft:sand",
            "minecraft:sandstone", "minecraft:gravel", "minecraft:ice", "minecraft:snow",
            "minecraft:snow_layer", "minecraft:lava", "minecraft:flowing_lava", "minecraft:grass_path",
            "minecraft:bedrock", "minecraft:end_portal_frame", "minecraft:end_portal",
            "minecraft:end_gateway", "minecraft:portal",
    };
}
