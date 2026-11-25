package anightdazingzoroark.riftedvessels.server.tileentities;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import valkyrienwarfare.api.IShipForces;

public class SubmarineTankTileEntity extends ShipLiftTileEntity {
    public static void applyVerticalForce(@NotNull IShipForces forces, double force) {
        forces.applyWorldForce(new Vec3d(0D, -forces.getWorldForce().y(), 0D));
        forces.applyBuoyancy(force);
    }
}
