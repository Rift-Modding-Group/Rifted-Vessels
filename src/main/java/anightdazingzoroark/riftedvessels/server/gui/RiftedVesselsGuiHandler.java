package anightdazingzoroark.riftedvessels.server.gui;

import anightdazingzoroark.riftedvessels.client.gui.ShipHelmGui;
import anightdazingzoroark.riftedvessels.server.blocks.RiftedVesselsBlocks;
import anightdazingzoroark.riftedvessels.server.tileentities.ShipHelmTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import org.jetbrains.annotations.Nullable;
import valkyrienwarfare.api.RiftedValkyriumAPI;

public class RiftedVesselsGuiHandler implements IGuiHandler {
    public static final int SHIP_HELM_GUI_ID = 0;

    @Override
    @Nullable
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    @Override
    @Nullable
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != SHIP_HELM_GUI_ID) return null;

        BlockPos receivedPos = new BlockPos(x, y, z);
        if (world.getBlockState(receivedPos).getBlock() != RiftedVesselsBlocks.SHIP_HELM) return null;

        TileEntity tileEntity = world.getTileEntity(receivedPos);
        boolean assemblyRequested;
        if (tileEntity instanceof ShipHelmTileEntity shipHelm) {
            assemblyRequested = shipHelm.isAssembled();
        }
        else {
            assemblyRequested = RiftedValkyriumAPI.getShipManagingBlock(world, receivedPos).isPresent();
        }
        return new ShipHelmGui(world, receivedPos, assemblyRequested);
    }
}
