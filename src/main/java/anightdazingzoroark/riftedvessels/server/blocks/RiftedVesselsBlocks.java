package anightdazingzoroark.riftedvessels.server.blocks;

import anightdazingzoroark.riftedvessels.RiftedVessels;
import anightdazingzoroark.riftedvessels.server.items.RiftedVesselsItems;
import net.minecraft.block.Block;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class RiftedVesselsBlocks {
    private static final List<Block> BLOCKS = new ArrayList<>();
    public static Block SHIP_HELM;

    public static void registerBlocks() {
        SHIP_HELM = registerBlock(new ShipHelmBlock(), "ship_helm", true, true);
    }

    private static <T extends Block> T registerBlock(T block, String registryName, boolean hasItem, boolean canBeInCreative) {
        if (canBeInCreative) block.setCreativeTab(RiftedVessels.creativeItemsTab);
        block.setRegistryName(registryName);
        block.setTranslationKey(registryName);
        BLOCKS.add(block);
        if (hasItem) RiftedVesselsItems.registerBlockItem(block, registryName, canBeInCreative);
        return block;
    }

    @SubscribeEvent
    public void onBlockRegistry(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(BLOCKS.toArray(new Block[0]));
    }
}
