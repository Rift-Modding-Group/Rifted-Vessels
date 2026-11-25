package anightdazingzoroark.riftedvessels.server.tileentities;

import anightdazingzoroark.riftedvessels.RiftedVesselsConfig;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import valkyrienwarfare.api.IPhysicsEntity;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class ShipLiftController {
    private static final Map<World, Map<UUID, ShipLiftController>> CONTROLLERS_BY_WORLD = Collections.synchronizedMap(new WeakHashMap<>());

    private final World world;
    private final UUID shipUuid;
    private final Set<ShipLiftTileEntity> liftBlocks;

    @Nullable
    private IPhysicsEntity ship;

    private ShipLiftController(@NotNull IPhysicsEntity ship) {
        this.world = ship.getWorld();
        this.shipUuid = ship.getShipData().getUuid();
        this.liftBlocks = ConcurrentHashMap.newKeySet();
        this.ship = ship;
    }

    @NotNull
    public static synchronized ShipLiftController attach(@NotNull IPhysicsEntity ship, @NotNull ShipLiftTileEntity liftBlock) {
        Map<UUID, ShipLiftController> worldControllers = CONTROLLERS_BY_WORLD.computeIfAbsent(
                ship.getWorld(), ignored -> new ConcurrentHashMap<>()
        );
        UUID shipUuid = ship.getShipData().getUuid();
        ShipLiftController controller = worldControllers.computeIfAbsent(shipUuid, ignored -> new ShipLiftController(ship));
        controller.ship = ship;
        controller.liftBlocks.add(liftBlock);
        return controller;
    }

    public static synchronized void release(@NotNull ShipLiftController controller, @NotNull ShipLiftTileEntity liftBlock) {
        controller.liftBlocks.remove(liftBlock);
        if (!controller.liftBlocks.isEmpty()) return;

        controller.ship = null;
        Map<UUID, ShipLiftController> worldControllers = CONTROLLERS_BY_WORLD.get(controller.world);
        if (worldControllers == null) return;
        worldControllers.remove(controller.shipUuid, controller);
        if (worldControllers.isEmpty()) CONTROLLERS_BY_WORLD.remove(controller.world);
    }

    @NotNull
    public static synchronized LiftMode getLiftMode(@NotNull IPhysicsEntity ship) {
        Map<UUID, ShipLiftController> worldControllers = CONTROLLERS_BY_WORLD.get(ship.getWorld());
        if (worldControllers == null) return LiftMode.FLOATER;
        ShipLiftController controller = worldControllers.get(ship.getShipData().getUuid());
        if (controller == null || controller.ship != ship) return LiftMode.FLOATER;

        int balloons = 0;
        int tanks = 0;
        for (ShipLiftTileEntity liftBlock : controller.liftBlocks) {
            if (liftBlock.isInvalid()) continue;
            if (liftBlock instanceof BalloonTileEntity) balloons++;
            else if (liftBlock instanceof SubmarineTankTileEntity) tanks++;
        }
        return selectLiftMode(balloons, tanks);
    }

    @NotNull
    public static LiftMode selectLiftMode(int balloons, int tanks) {
        if (balloons >= RiftedVesselsConfig.shipBalloonMinimumCount) return LiftMode.BALLOON;
        if (tanks >= RiftedVesselsConfig.shipSubmarineTankMinimumCount) return LiftMode.SUBMARINE;
        return LiftMode.FLOATER;
    }

    public enum LiftMode {
        FLOATER,
        SUBMARINE,
        BALLOON
    }

    public boolean manages(@NotNull IPhysicsEntity ship) {
        return this.ship == ship;
    }
}
