package anightdazingzoroark.riftedvessels;

import anightdazingzoroark.riftedvessels.server.ServerProxy;
import anightdazingzoroark.riftedvessels.server.blocks.RiftedVesselsBlocks;
import anightdazingzoroark.riftlib.RiftLib;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod(modid = RiftedVessels.MOD_ID, name = RiftedVessels.MOD_NAME, version = RiftedVessels.VERSION)
public class RiftedVessels {
    public static final String MOD_ID = "riftedvessels";
    public static final String MOD_NAME = "Rifted Vessels";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_NAME);
    public static final CreativeTabs creativeItemsTab = new CreativeTabs(MOD_ID) {
        @Override
        @SideOnly(Side.CLIENT)
        public ItemStack createIcon() {
            return new ItemStack(RiftedVesselsBlocks.SHIP_HELM);
        }

        @Override
        public String getTranslationKey() {
            return "itemGroup.rifted_vessels";
        }
    };

    @SidedProxy(
            modId = MOD_ID,
            clientSide = "anightdazingzoroark.riftedvessels.client.ClientProxy",
            serverSide = "anightdazingzoroark.riftedvessels.server.ServerProxy"
    )
    public static ServerProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Hello From {}!", MOD_NAME);
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        RiftLib.initialize();
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}