package anightdazingzoroark.riftedvessels.server.tileentities;

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

public class BalloonController {
    private static final Map<World, Map<UUID, BalloonController>> CONTROLLERS_BY_WORLD = Collections.synchronizedMap(new WeakHashMap<>());

    private final World world;
    private final UUID shipUuid;
    private final Set<BalloonTileEntity> balloons;

    @Nullable
    private IPhysicsEntity ship;

    private BalloonController(@NotNull IPhysicsEntity ship) {
        this.world = ship.getWorld();
        this.shipUuid = ship.getShipData().getUuid();
        this.balloons = ConcurrentHashMap.newKeySet();
        this.ship = ship;
    }

    @NotNull
    public static synchronized BalloonController attach(@NotNull IPhysicsEntity ship, @NotNull BalloonTileEntity balloon) {
        Map<UUID, BalloonController> worldControllers = CONTROLLERS_BY_WORLD.computeIfAbsent(
                ship.getWorld(), ignored -> new ConcurrentHashMap<>()
        );
        UUID shipUuid = ship.getShipData().getUuid();
        BalloonController controller = worldControllers.computeIfAbsent(shipUuid, ignored -> new BalloonController(ship));
        controller.ship = ship;
        controller.balloons.add(balloon);
        return controller;
    }

    public static synchronized void release(@NotNull BalloonController controller, @NotNull BalloonTileEntity balloon) {
        controller.balloons.remove(balloon);
        if (!controller.balloons.isEmpty()) return;

        controller.ship = null;
        Map<UUID, BalloonController> worldControllers = CONTROLLERS_BY_WORLD.get(controller.world);
        if (worldControllers == null) return;
        worldControllers.remove(controller.shipUuid, controller);
        if (worldControllers.isEmpty()) CONTROLLERS_BY_WORLD.remove(controller.world);
    }

    public static synchronized int getBalloonCount(@NotNull IPhysicsEntity ship) {
        Map<UUID, BalloonController> worldControllers = CONTROLLERS_BY_WORLD.get(ship.getWorld());
        if (worldControllers == null) return 0;
        BalloonController controller = worldControllers.get(ship.getShipData().getUuid());
        if (controller == null || controller.ship != ship) return 0;

        int count = 0;
        for (BalloonTileEntity balloon : controller.balloons) {
            if (!balloon.isInvalid()) count++;
        }
        return count;
    }

    public boolean manages(@NotNull IPhysicsEntity ship) {
        return this.ship == ship;
    }
}
