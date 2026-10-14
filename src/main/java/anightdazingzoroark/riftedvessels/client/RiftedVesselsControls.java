package anightdazingzoroark.riftedvessels.client;

import anightdazingzoroark.riftedvessels.server.ServerProxy;
import anightdazingzoroark.riftedvessels.server.message.ControlShipMessage;
import net.minecraft.util.MovementInput;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class RiftedVesselsControls {
    private static boolean controllingShip;

    @SubscribeEvent
    public void onInputUpdate(@NotNull InputUpdateEvent event) {
        if (!controllingShip) return;

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

        //cancel player movement
        input.moveForward = 0f;
        input.moveStrafe = 0f;

        //exit controlled ship
        if (input.sneak) {
            input.sneak = false;
            controllingShip = false;
            ServerProxy.MESSAGE_WRAPPER.sendToServer(new ControlShipMessage(true));
        }
        //send controls to the ship
        else ServerProxy.MESSAGE_WRAPPER.sendToServer(new ControlShipMessage(forward, turn));
    }

    public static void setControllingShip(boolean controlling) {
        controllingShip = controlling;
    }
}
