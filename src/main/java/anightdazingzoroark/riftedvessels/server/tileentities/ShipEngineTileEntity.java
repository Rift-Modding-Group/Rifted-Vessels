package anightdazingzoroark.riftedvessels.server.tileentities;

import anightdazingzoroark.riftlib.nbtStorageUser.propertyValue.IntegerPropertyValue;
import anightdazingzoroark.riftlib.nbtStorageUser.tileEntity.RiftTileEntity;
import net.minecraft.util.ITickable;

public class ShipEngineTileEntity extends RiftTileEntity implements ITickable {
    @Override
    public void update() {

    }

    @Override
    public void registerValues() {
        this.registerValue(new IntegerPropertyValue("Ignition", 0));
    }

    public void setIgnition(int value) {
        this.setValue("Ignition", value);
    }

    public int getIgnition() {
        return this.getValue("Ignition");
    }

    public boolean isIgnited() {
        return this.getIgnition() > 0;
    }
}
