package anightdazingzoroark.riftedvessels.server.message;

import anightdazingzoroark.riftedvessels.client.gui.ShipHelmGui;
import anightdazingzoroark.riftlib.message.RiftLibMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class OpenShipHelmGuiMessage extends RiftLibMessage<OpenShipHelmGuiMessage> {
    private BlockPos shipHelmPos;
    private boolean assemblyRequested;

    public OpenShipHelmGuiMessage() {}

    public OpenShipHelmGuiMessage(BlockPos shipHelmPos, boolean assemblyRequested) {
        this.shipHelmPos = shipHelmPos;
        this.assemblyRequested = assemblyRequested;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.shipHelmPos = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
        this.assemblyRequested = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(this.shipHelmPos.getX());
        buffer.writeInt(this.shipHelmPos.getY());
        buffer.writeInt(this.shipHelmPos.getZ());
        buffer.writeBoolean(this.assemblyRequested);
    }

    @Override
    public void executeOnServer(MinecraftServer server, OpenShipHelmGuiMessage message, EntityPlayer player, MessageContext messageContext) {}

    @Override
    public void executeOnClient(Minecraft minecraft, OpenShipHelmGuiMessage message, EntityPlayer player, MessageContext messageContext) {
        if (minecraft.world == null || message.shipHelmPos == null) return;
        minecraft.displayGuiScreen(new ShipHelmGui(
                minecraft.world,
                message.shipHelmPos,
                message.assemblyRequested
        ));
    }
}
