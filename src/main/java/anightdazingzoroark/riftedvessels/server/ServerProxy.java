package anightdazingzoroark.riftedvessels.server;

import anightdazingzoroark.riftedvessels.RiftedVessels;
import anightdazingzoroark.riftedvessels.client.RiftedVesselsSounds;
import anightdazingzoroark.riftedvessels.server.blocks.RiftedVesselsBlocks;
import anightdazingzoroark.riftedvessels.server.items.RiftedVesselsItems;
import anightdazingzoroark.riftedvessels.server.message.*;
import anightdazingzoroark.riftedvessels.server.tileentities.*;
import anightdazingzoroark.riftlib.message.RiftLibMessage;
import anightdazingzoroark.riftlib.message.RiftLibMessageSide;
import anightdazingzoroark.riftlib.message.RiftLibMessageWrapper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import valkyrienwarfare.api.BlockMass;
import valkyrienwarfare.api.RiftedValkyriumAPI;

public class ServerProxy {
    public static final RiftLibMessageWrapper<RiftLibMessage, RiftLibMessage> MESSAGE_WRAPPER = new RiftLibMessageWrapper<>(RiftedVessels.MOD_ID);

    public void preInit(FMLPreInitializationEvent event) {
        //---registries---
        RiftedVesselsBlocks.registerBlocks();

        //---messages---
        MESSAGE_WRAPPER.registerMessage(OpenShipHelmGuiMessage.class, RiftLibMessageSide.CLIENT);
        MESSAGE_WRAPPER.registerMessage(ShipControlStateMessage.class, RiftLibMessageSide.CLIENT);
        MESSAGE_WRAPPER.registerMessage(ControlShipMessage.class, RiftLibMessageSide.SERVER);
        MESSAGE_WRAPPER.registerMessage(AssembleShipMessage.class, RiftLibMessageSide.SERVER);
        MESSAGE_WRAPPER.registerMessage(AlignShipMessage.class, RiftLibMessageSide.SERVER);
        MESSAGE_WRAPPER.registerMessage(CruiseControlShipMessage.class, RiftLibMessageSide.SERVER);

        //---events---
        MinecraftForge.EVENT_BUS.register(new RiftedVesselsBlocks());
        MinecraftForge.EVENT_BUS.register(new RiftedVesselsItems());
        MinecraftForge.EVENT_BUS.register(new RiftedVesselsSounds());
    }

    public void init(FMLInitializationEvent event) {
        //---tile entities---
        GameRegistry.registerTileEntity(
                ShipHelmTileEntity.class,
                new ResourceLocation(RiftedVessels.MOD_ID, "ship_helm")
        );
        GameRegistry.registerTileEntity(
                ShipEngineTileEntity.class,
                new ResourceLocation(RiftedVessels.MOD_ID, "ship_engine")
        );
        GameRegistry.registerTileEntity(FloaterTileEntity.class, new ResourceLocation(RiftedVessels.MOD_ID, "floater"));
        GameRegistry.registerTileEntity(BalloonTileEntity.class, new ResourceLocation(RiftedVessels.MOD_ID, "balloon"));
        GameRegistry.registerTileEntity(AnchorTileEntity.class, new ResourceLocation(RiftedVessels.MOD_ID, "anchor"));

        //---custom block weights---
        RiftedValkyriumAPI.setBlockMass(RiftedVesselsBlocks.SHIP_HELM, BlockMass.NONE);
        RiftedValkyriumAPI.setBlockMass(RiftedVesselsBlocks.BALLOON, BlockMass.VERY_LIGHT);
    }

    public void postInit(FMLPostInitializationEvent event) {}
}
