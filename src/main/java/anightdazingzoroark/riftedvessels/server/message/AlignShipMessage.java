package anightdazingzoroark.riftedvessels.server.message;

import anightdazingzoroark.riftlib.message.RiftLibMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class AlignShipMessage extends RiftLibMessage<AlignShipMessage> {
    public AlignShipMessage() {}

    @Override
    public void fromBytes(ByteBuf byteBuf) {

    }

    @Override
    public void toBytes(ByteBuf byteBuf) {

    }

    @Override
    public void executeOnServer(MinecraftServer minecraftServer, AlignShipMessage alignShipMessage, EntityPlayer entityPlayer, MessageContext messageContext) {

    }

    @Override
    public void executeOnClient(Minecraft minecraft, AlignShipMessage alignShipMessage, EntityPlayer entityPlayer, MessageContext messageContext) {

    }
}
