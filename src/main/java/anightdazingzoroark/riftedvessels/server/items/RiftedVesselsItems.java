package anightdazingzoroark.riftedvessels.server.items;

import anightdazingzoroark.riftedvessels.RiftedVessels;
import anightdazingzoroark.riftedvessels.server.blocks.BalloonBlock;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RiftedVesselsItems {
    public static final List<Item> ITEMS = new ArrayList<>();
    public static final Map<Block, ItemBlock> BLOCK_ITEMS = new HashMap<>();
    
    public static ItemBlock getBlockItem(Block block) {
        return BLOCK_ITEMS.get(block);
    }

    public static <T extends Item> T registerItem(T item, String registryName, boolean canBeInCreative) {
        if (canBeInCreative) item.setCreativeTab(RiftedVessels.creativeItemsTab);
        if (item.getRegistryName() == null) item.setRegistryName(registryName);
        item.setTranslationKey(RiftedVessels.MOD_ID + "." + registryName);
        ITEMS.add(item);
        return item;
    }

    public static void registerBlockItem(Block block, String registryName, boolean itemStackable, boolean canBeInCreative) {
        ItemBlock blockItem;
        if (block instanceof BalloonBlock balloonBlock) blockItem = new BalloonItemBlock(balloonBlock);
        else blockItem = new ItemBlock(block);

        if (!itemStackable) blockItem.setMaxStackSize(1);
        registerItem(blockItem, registryName, canBeInCreative);
        BLOCK_ITEMS.put(block, blockItem);
    }

    @SubscribeEvent
    public void onItemRegistry(RegistryEvent.Register<Item> e) {
        IForgeRegistry<Item> reg = e.getRegistry();
        reg.registerAll(ITEMS.toArray(new Item[0]));
    }
}
