package anightdazingzoroark.riftedvessels.client;

import anightdazingzoroark.riftedvessels.RiftedVessels;
import anightdazingzoroark.riftedvessels.server.ServerProxy;
import anightdazingzoroark.riftedvessels.server.message.ControlShipMessage;
import anightdazingzoroark.riftedvessels.server.message.CruiseControlShipMessage;
import anightdazingzoroark.riftedvessels.server.tileentities.ShipHelmTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;

/**
 * Handle controls on the ship and store keybinds
 * */
public class RiftedVesselsControls {
    public static final KeyBinding CRUISE_CONTROL = new KeyBinding("key.riftedvessels.cruise_control", Keyboard.KEY_C, RiftedVessels.MOD_NAME);
    public static final KeyBinding SHIP_DESCEND = new KeyBinding("key.riftedvessels.ship_descend", Keyboard.KEY_X, RiftedVessels.MOD_NAME);

    @Nullable
    private static BlockPos controlledShipHelmPos;

    @SubscribeEvent
    public void onInputUpdate(@NotNull InputUpdateEvent event) {
        if (controlledShipHelmPos == null) return;

        //forward and back
        MovementInput input = event.getMovementInput();
        int forward;
        if (input.moveForward > 0f) forward = 1;
        else if (input.moveForward < 0f) forward = -1;
        else forward = 0;

        //left and right
        int turn;
        if (input.moveStrafe > 0F) turn = 1;
        else if (input.moveStrafe < 0F) turn = -1;
        else turn = 0;

        //up and down
        int vertical;
        boolean ascend = input.jump;
        boolean descend = SHIP_DESCEND.isKeyDown();
        if (ascend == descend) vertical = 0;
        else if (ascend) vertical = 1;
        else vertical = -1;

        //cancel player movement
        input.moveForward = 0f;
        input.moveStrafe = 0f;
        input.jump = false;

        //exit controlled ship
        if (input.sneak) {
            input.sneak = false;
            setControllingShip(null);
            ServerProxy.MESSAGE_WRAPPER.sendToServer(new ControlShipMessage(true));
        }
        //send controls to the ship
        else ServerProxy.MESSAGE_WRAPPER.sendToServer(new ControlShipMessage(forward, turn, vertical));
    }

    @SubscribeEvent
    public void onKeyInput(@NotNull InputEvent.KeyInputEvent event) {
        if (!CRUISE_CONTROL.isPressed() || controlledShipHelmPos == null) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.world == null) return;

        TileEntity tileEntity = minecraft.world.getTileEntity(controlledShipHelmPos);
        if (!(tileEntity instanceof ShipHelmTileEntity shipHelm)) return;

        boolean cruiseControl = !shipHelm.getCruiseControlling();

        //send player notif in action bar
        if (cruiseControl) {
            minecraft.player.sendStatusMessage(new TextComponentTranslation("warning.riftedvessels.cruise_control_enabled"), true);
        }
        else {
            minecraft.player.sendStatusMessage(new TextComponentTranslation("warning.riftedvessels.cruise_control_disabled"), true);
        }

        //now send to server
        ServerProxy.MESSAGE_WRAPPER.sendToServer(new CruiseControlShipMessage(cruiseControl, controlledShipHelmPos));
    }

    public static void setControllingShip(@Nullable BlockPos shipHelmPos) {
        controlledShipHelmPos = shipHelmPos;
    }
}