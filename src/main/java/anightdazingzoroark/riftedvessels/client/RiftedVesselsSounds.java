package anightdazingzoroark.riftedvessels.client;

import anightdazingzoroark.riftedvessels.RiftedVessels;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class RiftedVesselsSounds {
    public static SoundEvent SHIP_ENGINE_IGNITE = createSoundEvent("block.riftedvessels.shipengine.ignite");
    public static SoundEvent SHIP_ENGINE_REMOVE_FUEL = createSoundEvent("block.riftedvessels.shipengine.remove_fuel");

    private static SoundEvent createSoundEvent(String name) {
        ResourceLocation location = new ResourceLocation(RiftedVessels.MOD_ID, name);
        return new SoundEvent(location).setRegistryName(location);
    }

    @SubscribeEvent
    public void onSoundRegistry(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(SHIP_ENGINE_IGNITE, SHIP_ENGINE_REMOVE_FUEL);
    }
}
