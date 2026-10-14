package anightdazingzoroark.riftedvessels.client;

import anightdazingzoroark.riftedvessels.RiftedVessels;
import anightdazingzoroark.riftedvessels.client.renderer.block.ShipHelmBlockRenderer;
import anightdazingzoroark.riftedvessels.client.renderer.item.ShipHelmItemRenderer;
import anightdazingzoroark.riftedvessels.server.ServerProxy;
import anightdazingzoroark.riftedvessels.server.blocks.RiftedVesselsBlocks;
import anightdazingzoroark.riftedvessels.server.gui.RiftedVesselsGuiHandler;
import anightdazingzoroark.riftedvessels.server.items.RiftedVesselsItems;
import anightdazingzoroark.riftlib.renderers.geo.GeoBlockRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class ClientProxy extends ServerProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        MinecraftForge.EVENT_BUS.register(new RiftedVesselsControls());

        //---block rendering---
        GeoBlockRenderer.registerBlockRenderer(RiftedVesselsBlocks.SHIP_HELM, new ShipHelmBlockRenderer());

        //---item rendering---
        RiftedVesselsItems.getBlockItem(RiftedVesselsBlocks.SHIP_HELM).setTileEntityItemStackRenderer(new ShipHelmItemRenderer());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        //---register item models---
        for (Item item : RiftedVesselsItems.ITEMS) {
            final String resName = item.getRegistryName().toString();
            final ModelResourceLocation res = new ModelResourceLocation(resName, "inventory");
            Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, 0, res);
        }

        //---GUIs---
        NetworkRegistry.INSTANCE.registerGuiHandler(RiftedVessels.INSTANCE, new RiftedVesselsGuiHandler());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }
}
