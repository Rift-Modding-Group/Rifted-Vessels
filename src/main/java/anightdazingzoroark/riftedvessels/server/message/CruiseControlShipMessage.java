package anightdazingzoroark.riftedvessels.server.message;

import anightdazingzoroark.riftedvessels.server.tileentities.ShipHelmTileEntity;
import anightdazingzoroark.riftlib.message.RiftLibMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jetbrains.annotations.NotNull;
import valkyrienwarfare.api.RiftedValkyriumAPI;

public class CruiseControlShipMessage extends RiftLibMessage<CruiseControlShipMessage> {
    private boolean cruiseControl;
    private BlockPos shipHelmPos;

    public CruiseControlShipMessage() {}

    public CruiseControlShipMessage(boolean cruiseControl, @NotNull BlockPos shipHelmPos) {
        this.cruiseControl = cruiseControl;
        this.shipHelmPos = shipHelmPos;
    }

    @Override
    public void fromBytes(@NotNull ByteBuf buf) {
        this.cruiseControl = buf.readBoolean();
        this.shipHelmPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public void toBytes(@NotNull ByteBuf buf) {
        buf.writeBoolean(this.cruiseControl);
        buf.writeInt(this.shipHelmPos.getX());
        buf.writeInt(this.shipHelmPos.getY());
        buf.writeInt(this.shipHelmPos.getZ());
    }

    @Override
    public void executeOnServer(
            @NotNull MinecraftServer server, @NotNull CruiseControlShipMessage message,
            @NotNull EntityPlayer entityPlayer, @NotNull MessageContext messageContext
    ) {
        BlockPos shipHelmPos = message.shipHelmPos;
        if (shipHelmPos == null || !(entityPlayer instanceof EntityPlayerMP serverPlayer)) return;

        World world = entityPlayer.getEntityWorld();
        TileEntity tileEntity = world.getTileEntity(shipHelmPos);
        if (!(tileEntity instanceof ShipHelmTileEntity shipHelm)) return;
        if (!shipHelm.isAssembled() || !RiftedValkyriumAPI.isPosInShipyard(world, shipHelmPos)) return;
        if (!shipHelm.isPlayerWithinControlDistance(serverPlayer)) return;

        shipHelm.setCruiseControlling(message.cruiseControl);
    }

    @Override
    public void executeOnClient(
            @NotNull Minecraft minecraft, @NotNull CruiseControlShipMessage message,
            @NotNull EntityPlayer entityPlayer, @NotNull MessageContext messageContext
    ) {}
}
