package anightdazingzoroark.riftedvessels.server.gui;

import anightdazingzoroark.riftedvessels.client.gui.ShipHelmGui;
import anightdazingzoroark.riftedvessels.server.tileentities.ShipHelmTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import org.jetbrains.annotations.Nullable;

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
        TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));
        if (id == SHIP_HELM_GUI_ID && tileEntity instanceof ShipHelmTileEntity shipHelm) {
            return new ShipHelmGui(shipHelm);
        }
        return null;
    }
}
