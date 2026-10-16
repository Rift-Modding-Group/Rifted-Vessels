package anightdazingzoroark.riftedvessels.client;

import anightdazingzoroark.riftedvessels.RiftedVessels;
import anightdazingzoroark.riftedvessels.client.renderer.block.AnchorBlockRenderer;
import anightdazingzoroark.riftedvessels.client.renderer.block.ShipHelmBlockRenderer;
import anightdazingzoroark.riftedvessels.client.renderer.item.AnchorItemRenderer;
import anightdazingzoroark.riftedvessels.client.renderer.item.ShipHelmItemRenderer;
import anightdazingzoroark.riftedvessels.server.ServerProxy;
import anightdazingzoroark.riftedvessels.server.blocks.RiftedVesselsBlocks;
import anightdazingzoroark.riftedvessels.server.gui.RiftedVesselsGuiHandler;
import anightdazingzoroark.riftedvessels.server.items.RiftedVesselsItems;
import anightdazingzoroark.riftlib.renderers.geo.GeoBlockRenderer;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ClientProxy extends ServerProxy {
    @SideOnly(Side.CLIENT)
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        MinecraftForge.EVENT_BUS.register(new RiftedVesselsControls());

        //---block rendering---
        GeoBlockRenderer.registerBlockRenderer(RiftedVesselsBlocks.SHIP_HELM, new ShipHelmBlockRenderer());
        GeoBlockRenderer.registerBlockRenderer(RiftedVesselsBlocks.ANCHOR, new AnchorBlockRenderer());

        //---item rendering---
        RiftedVesselsItems.getBlockItem(RiftedVesselsBlocks.SHIP_HELM).setTileEntityItemStackRenderer(new ShipHelmItemRenderer());
        RiftedVesselsItems.getBlockItem(RiftedVesselsBlocks.ANCHOR).setTileEntityItemStackRenderer(new AnchorItemRenderer());

        //---balloon item rendering---
        Item balloon = RiftedVesselsItems.getBlockItem(RiftedVesselsBlocks.BALLOON);
        for (EnumDyeColor color : EnumDyeColor.values()) {
            ModelLoader.setCustomModelResourceLocation(
                    balloon, color.getMetadata(),
                    new ModelResourceLocation(RiftedVessels.MOD_ID + ":balloon_" + color.getName(), "inventory")
            );
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        //---controls---
        ClientRegistry.registerKeyBinding(RiftedVesselsControls.CRUISE_CONTROL);
        ClientRegistry.registerKeyBinding(RiftedVesselsControls.SHIP_DESCEND);

        //---register item models---
        for (Item item : RiftedVesselsItems.ITEMS) {
            if (item == RiftedVesselsItems.getBlockItem(RiftedVesselsBlocks.BALLOON)) continue;
            String resName = item.getRegistryName().toString();
            ModelResourceLocation res = new ModelResourceLocation(resName, "inventory");
            Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, 0, res);
        }

        //---register solid block models---
        for (Block block : RiftedVesselsBlocks.BLOCKS) {
            //special case for ballons due to using custom metadata for colors
            if (block == RiftedVesselsBlocks.BALLOON) {
                for (EnumDyeColor color : EnumDyeColor.values()) {
                    ModelLoader.setCustomModelResourceLocation(
                            Item.getItemFromBlock(block), color.getMetadata(),
                            new ModelResourceLocation(RiftedVessels.MOD_ID + ":balloon_" + color.getName(), "inventory")
                    );
                }
            }
            //everythin else
            else {
                ModelLoader.setCustomModelResourceLocation(
                        Item.getItemFromBlock(block), 0,
                        new ModelResourceLocation(block.getRegistryName().toString(), "inventory")
                );
            }
        }

        //---GUIs---
        NetworkRegistry.INSTANCE.registerGuiHandler(RiftedVessels.INSTANCE, new RiftedVesselsGuiHandler());
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }
}
