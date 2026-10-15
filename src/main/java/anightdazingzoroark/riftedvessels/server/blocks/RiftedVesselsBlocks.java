package anightdazingzoroark.riftedvessels.server.blocks;

import anightdazingzoroark.riftedvessels.RiftedVessels;
import anightdazingzoroark.riftedvessels.server.items.RiftedVesselsItems;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class RiftedVesselsBlocks {
    public static final List<Block> BLOCKS = new ArrayList<>();
    public static Block SHIP_HELM;
    public static Block SHIP_ENGINE;
    public static Block FLOATER;
    //public static Block BALLOON;
    //public static Block ANCHOR;

    public static void registerBlocks() {
        SHIP_HELM = registerBlock(new ShipHelmBlock(), "ship_helm", true, false, true);
        SHIP_ENGINE = registerBlock(new ShipEngineBlock(), "ship_engine", true, true,true);
        FLOATER = registerBlock(new FloaterBlock(), "floater", true, true, true);
    }

    private static <T extends Block> T registerBlock(T block, String registryName, boolean hasItem, boolean itemStackable, boolean canBeInCreative) {
        if (canBeInCreative) block.setCreativeTab(RiftedVessels.creativeItemsTab);
        block.setRegistryName(registryName);
        block.setTranslationKey(RiftedVessels.MOD_ID + "." + registryName);
        BLOCKS.add(block);
        if (hasItem) RiftedVesselsItems.registerBlockItem(block, registryName, itemStackable, canBeInCreative);
        return block;
    }

    @SubscribeEvent
    public void onBlockRegistry(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(BLOCKS.toArray(new Block[0]));
    }
}
