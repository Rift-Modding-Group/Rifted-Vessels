package anightdazingzoroark.riftedvessels.server.tileentities;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import valkyrienwarfare.api.IPhysicsEntity;
import valkyrienwarfare.api.RiftedValkyriumAPI;

import java.util.Optional;

public class BalloonTileEntity extends TileEntity implements ITickable {
    @Nullable
    private BalloonController controller;

    @Override
    public void update() {
        if (this.world.isRemote) return;

        Optional<IPhysicsEntity> ship = RiftedValkyriumAPI.getPhysoManagingBlock(this.world, this.pos);
        if (ship.isPresent()) this.setControllerShip(ship.get());
        else this.clearController();
    }

    private void setControllerShip(@NotNull IPhysicsEntity ship) {
        if (this.controller != null && this.controller.manages(ship)) return;
        this.clearController();
        this.controller = BalloonController.attach(ship, this);
    }

    private void clearController() {
        if (this.controller == null) return;
        BalloonController previousController = this.controller;
        this.controller = null;
        BalloonController.release(previousController, this);
    }

    @Override
    public void invalidate() {
        this.clearController();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        this.clearController();
        super.onChunkUnload();
    }
}
