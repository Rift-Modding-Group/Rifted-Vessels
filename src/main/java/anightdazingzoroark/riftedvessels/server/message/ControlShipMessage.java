package anightdazingzoroark.riftedvessels.server.message;

import anightdazingzoroark.riftedvessels.server.tileentities.ShipHelmController;
import anightdazingzoroark.riftedvessels.server.tileentities.ShipHelmTileEntity;
import anightdazingzoroark.riftlib.message.RiftLibMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jetbrains.annotations.NotNull;

public class ControlShipMessage extends RiftLibMessage<ControlShipMessage> {
    private int forward;
    private int turn;
    private int vertical;
    private boolean stopControlling;

    public ControlShipMessage() {}

    public ControlShipMessage(int forward, int turn, int vertical) {
        this.forward = Math.clamp(forward, -1, 1);
        this.turn = Math.clamp(turn, -1, 1);
        this.vertical = Math.clamp(vertical, -1, 1);
    }

    public ControlShipMessage(boolean stopControlling) {
        this.stopControlling = stopControlling;
    }

    @Override
    public void fromBytes(@NotNull ByteBuf buffer) {
        this.forward = buffer.readByte();
        this.turn = buffer.readByte();
        this.vertical = buffer.readByte();
        this.stopControlling = buffer.readBoolean();
    }

    @Override
    public void toBytes(@NotNull ByteBuf buffer) {
        buffer.writeByte(this.forward);
        buffer.writeByte(this.turn);
        buffer.writeByte(this.vertical);
        buffer.writeBoolean(this.stopControlling);
    }

    @Override
    public void executeOnServer(
            @NotNull MinecraftServer server, @NotNull ControlShipMessage message,
            @NotNull EntityPlayer player, @NotNull MessageContext context
    ) {
        if (!(player instanceof EntityPlayerMP serverPlayer)) return;
        if (message.forward < -1 || message.forward > 1
                || message.turn < -1 || message.turn > 1
                || message.vertical < -1 || message.vertical > 1
        ) {
            return;
        }
        ShipHelmController.handleControlInput(serverPlayer, message.forward, message.turn, message.vertical, message.stopControlling);
    }

    @Override
    public void executeOnClient(
            @NotNull Minecraft minecraft, @NotNull ControlShipMessage message,
            @NotNull EntityPlayer player, @NotNull MessageContext context
    ) {}
}
