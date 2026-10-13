package anightdazingzoroark.riftedvessels.server;

import anightdazingzoroark.riftedvessels.server.blocks.RiftedVesselsBlocks;
import anightdazingzoroark.riftedvessels.server.items.RiftedVesselsItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ServerProxy {
    public void preInit(FMLPreInitializationEvent event) {
        //---registries---
        RiftedVesselsBlocks.registerBlocks();

        //---events---
        MinecraftForge.EVENT_BUS.register(new RiftedVesselsBlocks());
        MinecraftForge.EVENT_BUS.register(new RiftedVesselsItems());
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}
}
