package anightdazingzoroark.riftedvessels.server.message;

import anightdazingzoroark.riftedvessels.RiftedVessels;
import anightdazingzoroark.riftedvessels.server.gui.RiftedVesselsGuiHandler;
import anightdazingzoroark.riftlib.message.RiftLibMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class OpenShipHelmGuiMessage extends RiftLibMessage<OpenShipHelmGuiMessage> {
    private BlockPos shipHelmPos;

    public OpenShipHelmGuiMessage() {}

    public OpenShipHelmGuiMessage(BlockPos shipHelmPos) {
        this.shipHelmPos = shipHelmPos;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.shipHelmPos = BlockPos.fromLong(buffer.readLong());
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(this.shipHelmPos.toLong());
    }

    @Override
    public void executeOnServer(
            MinecraftServer server, OpenShipHelmGuiMessage message,
            EntityPlayer player, MessageContext messageContext
    ) {}

    @Override
    public void executeOnClient(
            Minecraft minecraft, OpenShipHelmGuiMessage message,
            EntityPlayer player, MessageContext messageContext
    ) {
        if (minecraft.world == null || message.shipHelmPos == null) {
            return;
        }

        player.openGui(
                RiftedVessels.INSTANCE,
                RiftedVesselsGuiHandler.SHIP_HELM_GUI_ID,
                minecraft.world,
                message.shipHelmPos.getX(),
                message.shipHelmPos.getY(),
                message.shipHelmPos.getZ()
        );
    }
}
