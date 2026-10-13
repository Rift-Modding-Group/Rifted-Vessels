package anightdazingzoroark.riftedvessels.server;

import anightdazingzoroark.riftedvessels.RiftedVessels;
import anightdazingzoroark.riftedvessels.server.blocks.RiftedVesselsBlocks;
import anightdazingzoroark.riftedvessels.server.items.RiftedVesselsItems;
import anightdazingzoroark.riftedvessels.server.message.OpenShipHelmGuiMessage;
import anightdazingzoroark.riftedvessels.server.tileentities.ShipHelmTileEntity;
import anightdazingzoroark.riftlib.message.RiftLibMessage;
import anightdazingzoroark.riftlib.message.RiftLibMessageSide;
import anightdazingzoroark.riftlib.message.RiftLibMessageWrapper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class ServerProxy {
    public static final RiftLibMessageWrapper<RiftLibMessage, RiftLibMessage> MESSAGE_WRAPPER = new RiftLibMessageWrapper<>(RiftedVessels.MOD_ID);

    public void preInit(FMLPreInitializationEvent event) {
        //---registries---
        RiftedVesselsBlocks.registerBlocks();

        //---messages---
        MESSAGE_WRAPPER.registerMessage(OpenShipHelmGuiMessage.class, RiftLibMessageSide.CLIENT);

        //---events---
        MinecraftForge.EVENT_BUS.register(new RiftedVesselsBlocks());
        MinecraftForge.EVENT_BUS.register(new RiftedVesselsItems());
    }

    public void init(FMLInitializationEvent event) {
        //---tile entities---
        GameRegistry.registerTileEntity(
                ShipHelmTileEntity.class,
                new ResourceLocation(RiftedVessels.MOD_ID, "ship_helm")
        );
    }

    public void postInit(FMLPostInitializationEvent event) {}
}
