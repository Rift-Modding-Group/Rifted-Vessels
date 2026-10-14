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
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jetbrains.annotations.NotNull;
import valkyrienwarfare.api.RiftedValkyriumAPI;

public class StartControlShipMessage extends RiftLibMessage<StartControlShipMessage> {
    private BlockPos shipHelmPos;

    public StartControlShipMessage() {}

    public StartControlShipMessage(@NotNull BlockPos shipHelmPos) {
        this.shipHelmPos = shipHelmPos.toImmutable();
    }

    @Override
    public void fromBytes(@NotNull ByteBuf buffer) {
        this.shipHelmPos = BlockPos.fromLong(buffer.readLong());
    }

    @Override
    public void toBytes(@NotNull ByteBuf buffer) {
        buffer.writeLong(this.shipHelmPos.toLong());
    }

    @Override
    public void executeOnServer(
            @NotNull MinecraftServer server, @NotNull StartControlShipMessage message,
            @NotNull EntityPlayer player, @NotNull MessageContext context
    ) {
        if (!(player instanceof EntityPlayerMP serverPlayer) || message.shipHelmPos == null) return;

        TileEntity tileEntity = player.world.getTileEntity(message.shipHelmPos);
        if (!(tileEntity instanceof ShipHelmTileEntity shipHelm)) return;
        if (!shipHelm.isAssembled() || RiftedValkyriumAPI.getPhysoManagingBlock(player.world, message.shipHelmPos).isEmpty()) {
            player.sendStatusMessage(new TextComponentTranslation("warning.riftedvessels.not_assembled"), false);
        }
        else if (shipHelm.isBeingControlled()) {
            player.sendStatusMessage(new TextComponentTranslation("warning.riftedvessels.helm_occupied"), false);
        }
        else shipHelm.startControlling(serverPlayer);
    }

    @Override
    public void executeOnClient(
            @NotNull Minecraft minecraft, @NotNull StartControlShipMessage message,
            @NotNull EntityPlayer player, @NotNull MessageContext context
    ) {}
}
