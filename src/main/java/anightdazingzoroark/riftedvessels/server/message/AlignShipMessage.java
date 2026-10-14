package anightdazingzoroark.riftedvessels.server.message;

import anightdazingzoroark.riftedvessels.server.tileentities.ShipHelmTileEntity;
import anightdazingzoroark.riftlib.message.RiftLibMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jetbrains.annotations.NotNull;
import valkyrienwarfare.api.RiftedValkyriumAPI;

public class AlignShipMessage extends RiftLibMessage<AlignShipMessage> {
    private boolean align;
    private BlockPos shipHelmPos;

    public AlignShipMessage() {}

    public AlignShipMessage(boolean align, @NotNull BlockPos shipHelmPos) {
        this.align = align;
        this.shipHelmPos = shipHelmPos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.align = buf.readBoolean();
        this.shipHelmPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.align);
        buf.writeInt(this.shipHelmPos.getX());
        buf.writeInt(this.shipHelmPos.getY());
        buf.writeInt(this.shipHelmPos.getZ());
    }

    @Override
    public void executeOnServer(MinecraftServer server, AlignShipMessage message, EntityPlayer entityPlayer, MessageContext messageContext) {
        BlockPos shipHelmPos = message.shipHelmPos;
        if (shipHelmPos == null) return;

        World world = entityPlayer.getEntityWorld();
        TileEntity tileEntity = world.getTileEntity(shipHelmPos);
        if (!(tileEntity instanceof ShipHelmTileEntity shipHelm)) return;
        if (!shipHelm.isAssembled() || RiftedValkyriumAPI.getPhysoManagingBlock(world, shipHelmPos).isEmpty()) return;

        shipHelm.setAligning(message.align);
    }

    @Override
    public void executeOnClient(Minecraft minecraft, AlignShipMessage message, EntityPlayer entityPlayer, MessageContext messageContext) {}
}
