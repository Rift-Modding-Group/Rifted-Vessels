package anightdazingzoroark.riftedvessels.server.message;

import anightdazingzoroark.riftedvessels.client.RiftedVesselsControls;
import anightdazingzoroark.riftlib.message.RiftLibMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jetbrains.annotations.NotNull;

public class ShipControlStateMessage extends RiftLibMessage<ShipControlStateMessage> {
    private boolean controlling;

    public ShipControlStateMessage() {}

    public ShipControlStateMessage(boolean controlling) {
        this.controlling = controlling;
    }

    @Override
    public void fromBytes(@NotNull ByteBuf buffer) {
        this.controlling = buffer.readBoolean();
    }

    @Override
    public void toBytes(@NotNull ByteBuf buffer) {
        buffer.writeBoolean(this.controlling);
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
        RiftedVesselsControls.setControllingShip(message.controlling);
    }
}
