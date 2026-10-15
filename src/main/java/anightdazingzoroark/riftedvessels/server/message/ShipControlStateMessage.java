package anightdazingzoroark.riftedvessels.server.message;

import anightdazingzoroark.riftedvessels.client.RiftedVesselsControls;
import anightdazingzoroark.riftlib.message.RiftLibMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShipControlStateMessage extends RiftLibMessage<ShipControlStateMessage> {
    private boolean controlling;
    @Nullable
    private BlockPos shipHelmPos;

    public ShipControlStateMessage() {}

    public ShipControlStateMessage(boolean controlling) {
        this.controlling = controlling;
    }

    public ShipControlStateMessage(boolean controlling, @NotNull BlockPos shipHelmPos) {
        this.controlling = controlling;
        this.shipHelmPos = shipHelmPos;
    }

    @Override
    public void fromBytes(@NotNull ByteBuf buffer) {
        this.controlling = buffer.readBoolean();
        if (this.controlling) this.shipHelmPos = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
    }

    @Override
    public void toBytes(@NotNull ByteBuf buffer) {
        buffer.writeBoolean(this.controlling);
        if (this.controlling && this.shipHelmPos != null) {
            buffer.writeInt(this.shipHelmPos.getX());
            buffer.writeInt(this.shipHelmPos.getY());
            buffer.writeInt(this.shipHelmPos.getZ());
        }
    }

    @Override
    public void executeOnServer(
            @NotNull MinecraftServer server, @NotNull ShipControlStateMessage message,
            @NotNull EntityPlayer player, @NotNull MessageContext context
    ) {}

    @Override
    public void executeOnClient(
            @NotNull Minecraft minecraft, @NotNull ShipControlStateMessage message,
            @NotNull EntityPlayer player, @NotNull MessageContext context
    ) {
        RiftedVesselsControls.setControllingShip(message.controlling, message.shipHelmPos);
    }
}
