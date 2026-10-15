package anightdazingzoroark.riftedvessels.server.tileentities;

import anightdazingzoroark.riftedvessels.RiftedVesselsConfig;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import valkyrienwarfare.api.IPhysicsEntity;
import valkyrienwarfare.api.RiftedValkyriumAPI;
import valkyrienwarfare.api.TransformType;
import valkyrienwarfare.api.forces.IShipForces;
import valkyrienwarfare.api.forces.IShipForcesInducer;

import java.util.Optional;

public class FloaterTileEntity extends TileEntity implements IShipForcesInducer, ITickable {
    @Nullable
    private volatile IPhysicsEntity ship;
    private volatile int redstoneSignal;

    @Override
    public void update() {
        if (this.world.isRemote) return;

        this.redstoneSignal = Math.clamp(this.world.getRedstonePowerFromNeighbors(this.pos), 0, 15);

        Optional<IPhysicsEntity> optionalShip = RiftedValkyriumAPI.getPhysoManagingBlock(this.world, this.pos);
        IPhysicsEntity currentShip = optionalShip.orElse(null);
        if (this.ship == currentShip) return;

        this.detachFromShip();
        this.ship = currentShip;
        if (currentShip != null) currentShip.addForceInducer(this);
    }

    @Override
    public void applyForces(@NonNull IShipForces forces) {
        if (this.ship != forces.getShip() || this.isInvalid()) return;

        int signal = this.redstoneSignal;
        if (signal >= 15) return;
        double buoyancy = RiftedVesselsConfig.floaterBuoyancy * (15D - signal) / 15D;

        Vec3d floaterPosition = new Vec3d(
                this.pos.getX() + 0.5D,
                this.pos.getY() + 0.5D,
                this.pos.getZ() + 0.5D
        );

        if (RiftedVesselsConfig.floaterMustTouchWater) forces.applyBuoyancy(buoyancy, floaterPosition);
        else {
            Vec3d floaterWorldPosition = forces.getShip().transformVector(
                    floaterPosition,
                    TransformType.SUBSPACE_TO_GLOBAL
            );
            Vec3d buoyancyWorldPosition = new Vec3d(
                    floaterWorldPosition.x,
                    forces.getShip().getShipData().getShipBB().minY + 0.5D,
                    floaterWorldPosition.z
            );
            Vec3d buoyancyShipPosition = forces.getShip().transformVector(
                    buoyancyWorldPosition,
                    TransformType.GLOBAL_TO_SUBSPACE
            );
            forces.applyBuoyancy(buoyancy, buoyancyShipPosition);
        }
    }

    private void detachFromShip() {
        IPhysicsEntity previousShip = this.ship;
        this.ship = null;
        if (previousShip != null) previousShip.removeForceInducer(this);
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
