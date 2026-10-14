package anightdazingzoroark.riftedvessels.server.message;

import anightdazingzoroark.riftedvessels.server.RiftedVesselsUtil;
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
import valkyrienwarfare.api.RiftedValkyriumAPI;

public class AssembleShipMessage extends RiftLibMessage<AssembleShipMessage> {
    private boolean assemble; //if true, assemble; else, disassemble
    private BlockPos shipHelmPos; //ship helm position

    public AssembleShipMessage() {}

    public AssembleShipMessage(boolean assemble, BlockPos shipHelmPos) {
        this.assemble = assemble;
        this.shipHelmPos = shipHelmPos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.assemble = buf.readBoolean();
        this.shipHelmPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.assemble);
        buf.writeInt(this.shipHelmPos.getX());
        buf.writeInt(this.shipHelmPos.getY());
        buf.writeInt(this.shipHelmPos.getZ());
    }

    @Override
    public void executeOnServer(MinecraftServer server, AssembleShipMessage message, EntityPlayer entityPlayer, MessageContext messageContext) {
        BlockPos shipHelmPos = message.shipHelmPos;
        if (shipHelmPos == null) return;

        World world = entityPlayer.getEntityWorld();
        TileEntity tileEntity = world.getTileEntity(shipHelmPos);
        if (!(tileEntity instanceof ShipHelmTileEntity shipHelmTile)) return;

        boolean assembled = RiftedValkyriumAPI.getPhysoManagingBlock(world, shipHelmPos).isPresent();
        if (message.assemble == assembled) return;
        if (message.assemble && RiftedVesselsUtil.isBlockPosBlacklisted(world, shipHelmPos.down())) return;

        shipHelmTile.requestAssemblyChange(message.assemble);
    }

    @Override
    public void executeOnClient(Minecraft minecraft, AssembleShipMessage message, EntityPlayer entityPlayer, MessageContext messageContext) {}
}
