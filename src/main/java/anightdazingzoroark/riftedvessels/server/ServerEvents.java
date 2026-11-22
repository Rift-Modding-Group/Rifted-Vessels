package anightdazingzoroark.riftedvessels.server;

import anightdazingzoroark.riftedvessels.server.blocks.ShipEngineBlock;
import anightdazingzoroark.riftedvessels.server.tileentities.ShipEngineTileEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class ServerEvents {
    @SubscribeEvent
    public void onRightClickBlock(@NotNull PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntityPlayer().isSneaking() || !ShipEngineTileEntity.isFuel(event.getItemStack())) return;
        if (event.getUseBlock() == Event.Result.DENY) return;
        if (event.getWorld().getBlockState(event.getPos()).getBlock() instanceof ShipEngineBlock) {
            event.setUseBlock(Event.Result.ALLOW);
        }
    }
}
