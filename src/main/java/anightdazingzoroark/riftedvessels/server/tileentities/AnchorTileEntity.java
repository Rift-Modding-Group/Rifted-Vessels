package anightdazingzoroark.riftedvessels.server.tileentities;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import org.jetbrains.annotations.Nullable;
import valkyrienwarfare.api.IPhysicsEntity;
import valkyrienwarfare.api.RiftedValkyriumAPI;

import java.util.Optional;

public class AnchorTileEntity extends TileEntity implements ITickable {
    @Nullable
    private IPhysicsEntity ship;

    @Override
    public void update() {
        if (this.world.isRemote) return;

        //apply redstone to stop ship movement
        if (this.ship != null) {
            int redstonePower = Math.max(0, this.world.getRedstonePowerFromNeighbors(this.pos));
            boolean physicsEnabled = this.ship.isDeconstructing() || redstonePower == 0;
            this.ship.getShipData().setPhysicsEnabled(physicsEnabled);
        }

        //normal ship getting/setting beyond
        Optional<IPhysicsEntity> optionalShip = RiftedValkyriumAPI.getPhysoManagingBlock(this.world, this.pos);
        IPhysicsEntity currentShip = optionalShip.orElse(null);
        if (this.ship == currentShip) return;

        this.detachFromShip();
        this.ship = currentShip;
    }

    private void detachFromShip() {
        IPhysicsEntity previousShip = this.ship;
        this.ship = null;
        if (previousShip != null) previousShip.getShipData().setPhysicsEnabled(true);
    }

    @Override
    public void invalidate() {
        this.detachFromShip();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        this.detachFromShip();
        super.onChunkUnload();
    }
}
