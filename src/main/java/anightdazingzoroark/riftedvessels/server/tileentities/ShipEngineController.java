package anightdazingzoroark.riftedvessels.server.tileentities;

import anightdazingzoroark.riftedvessels.RiftedVesselsConfig;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3dc;
import valkyrienwarfare.api.IPhysicsEntity;
import valkyrienwarfare.api.IShipData;
import valkyrienwarfare.api.forces.IShipForces;
import valkyrienwarfare.api.forces.ShipForcesInducer;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects all engines on one ship and boosts its existing horizontal movement.
 */
public class ShipEngineController implements ShipForcesInducer {
    private static final double MINIMUM_BOOST_SPEED = 0.05D;
    private static final double MOVEMENT_CHANGE_THRESHOLD_PER_SECOND = 0.05D;
    private static final Map<World, Map<UUID, ShipEngineController>> CONTROLLERS_BY_WORLD = Collections.synchronizedMap(new WeakHashMap<>());

    private final World world;
    private final UUID shipUuid;
    private final Set<ShipEngineTileEntity> engines;

    @Nullable
    private IPhysicsEntity ship;
    private long lastServerTick = Long.MIN_VALUE;
    private volatile boolean attached;
    private volatile int burningEngineCount;
    private volatile double accelerationPerEngine;
    private volatile double maximumSpeed;
    private volatile boolean movementObserved;
    private volatile boolean movementBoostActive;
    private volatile double previousHorizontalSpeed;
    private volatile int previousBurningEngineCount;

    private ShipEngineController(@NotNull IPhysicsEntity ship) {
        this.world = ship.getWorld();
        this.shipUuid = ship.getShipData().getUuid();
        this.engines = ConcurrentHashMap.newKeySet();
        this.attachToShip(ship);
    }

    //---static helpers---
    @NotNull
    public static synchronized ShipEngineController attach(@NotNull IPhysicsEntity ship, @NotNull ShipEngineTileEntity engine) {
        Map<UUID, ShipEngineController> worldControllers = CONTROLLERS_BY_WORLD.computeIfAbsent(
                ship.getWorld(), ignored -> new ConcurrentHashMap<>()
        );
        UUID shipUuid = ship.getShipData().getUuid();
        ShipEngineController controller = worldControllers.computeIfAbsent(shipUuid, ignored -> new ShipEngineController(ship));
        controller.attachToShip(ship);
        controller.engines.add(engine);
        return controller;
    }

    public static synchronized void release(@NotNull ShipEngineController controller, @NotNull ShipEngineTileEntity engine) {
        controller.engines.remove(engine);
        if (!controller.engines.isEmpty()) return;

        controller.detachFromShip();
        Map<UUID, ShipEngineController> worldControllers = CONTROLLERS_BY_WORLD.get(controller.world);
        if (worldControllers == null) return;
        worldControllers.remove(controller.shipUuid, controller);
        if (worldControllers.isEmpty()) CONTROLLERS_BY_WORLD.remove(controller.world);
    }

    //---internal methods---
    private void attachToShip(@NotNull IPhysicsEntity ship) {
        if (this.ship == ship && this.attached) return;
        this.detachFromShip();
        this.ship = ship;
        this.attached = true;
        ship.addForceInducer(this);
    }

    private void detachFromShip() {
        this.attached = false;
        this.burningEngineCount = 0;
        this.maximumSpeed = 0D;
        this.resetMovementTracking();
        IPhysicsEntity previousShip = this.ship;
        this.ship = null;
        if (previousShip != null) previousShip.removeForceInducer(this);
    }

    public boolean manages(@NotNull IPhysicsEntity ship) {
        return this.ship == ship && this.attached;
    }

    public void tick() {
        if (!this.attached || this.ship == null || this.world.isRemote) return;
        long serverTick = this.world.getTotalWorldTime();
        if (this.lastServerTick == serverTick) return;
        this.lastServerTick = serverTick;

        int activeEngines = 0;
        for (ShipEngineTileEntity engine : this.engines) {
            if (!engine.isInvalid() && engine.isBurning()) activeEngines++;
        }

        this.accelerationPerEngine = RiftedVesselsConfig.shipEngineAcceleration;
        double speedBonus = Math.min(
                activeEngines * RiftedVesselsConfig.shipEngineSpeedBonus,
                RiftedVesselsConfig.shipEngineMaximumSpeedBonus
        );
        this.maximumSpeed = RiftedVesselsConfig.shipHelmMaximumSpeed + speedBonus;
        this.burningEngineCount = activeEngines;
    }

    //---force applications---
    @Override
    public void applyForces(@NotNull IShipForces forces) {
        IPhysicsEntity forceShip = forces.getShip();
        IShipData shipData = forceShip.getShipData();
        if (!this.attached || this.ship != forceShip || forceShip.isDeconstructing() || forceShip.isShipAligningToGrid() || !shipData.isPhysicsEnabled()) {
            this.resetMovementTracking();
            return;
        }

        double mass = shipData.getMass();
        double configuredAcceleration = this.accelerationPerEngine;
        double configuredMaximumSpeed = this.maximumSpeed;
        int activeEngines = this.burningEngineCount;
        if (mass <= 0D || activeEngines <= 0 || configuredAcceleration <= 0D || configuredMaximumSpeed <= 0D) {
            this.resetMovementTracking();
            return;
        }

        double timeStep = forces.getTimeStepSeconds();
        if (timeStep <= 0D) return;
        Vector3dc linearVelocity = shipData.getLinearVelocity();
        double horizontalSpeed = Math.sqrt(linearVelocity.x() * linearVelocity.x() + linearVelocity.z() * linearVelocity.z());
        if (activeEngines != this.previousBurningEngineCount) {
            this.movementObserved = false;
            this.previousBurningEngineCount = activeEngines;
        }
        this.updateMovementBoostState(horizontalSpeed, timeStep);
        if (!this.movementBoostActive || horizontalSpeed >= configuredMaximumSpeed) return;

        double directionX = linearVelocity.x() / horizontalSpeed;
        double directionZ = linearVelocity.z() / horizontalSpeed;
        double forceMagnitude = mass * configuredAcceleration * activeEngines;
        forces.applyWorldForce(new Vec3d(
                directionX * forceMagnitude,
                0D,
                directionZ * forceMagnitude
        ));
    }

    private void updateMovementBoostState(double horizontalSpeed, double timeStep) {
        if (horizontalSpeed < MINIMUM_BOOST_SPEED) {
            this.movementObserved = true;
            this.movementBoostActive = false;
            this.previousHorizontalSpeed = horizontalSpeed;
        }
        else if (!this.movementObserved) {
            this.movementObserved = true;
            this.movementBoostActive = true;
            this.previousHorizontalSpeed = horizontalSpeed;
        }
        else {
            double speedChange = horizontalSpeed - this.previousHorizontalSpeed;
            double movementChangeThreshold = MOVEMENT_CHANGE_THRESHOLD_PER_SECOND * timeStep;
            if (speedChange < -movementChangeThreshold) this.movementBoostActive = false;
            else if (speedChange > movementChangeThreshold) this.movementBoostActive = true;
            this.previousHorizontalSpeed = horizontalSpeed;
        }
    }

    private void resetMovementTracking() {
        this.movementObserved = false;
        this.movementBoostActive = false;
        this.previousHorizontalSpeed = 0D;
        this.previousBurningEngineCount = 0;
    }
}
