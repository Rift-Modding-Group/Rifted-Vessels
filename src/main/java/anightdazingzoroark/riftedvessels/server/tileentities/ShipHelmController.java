package anightdazingzoroark.riftedvessels.server.tileentities;

import anightdazingzoroark.riftedvessels.RiftedVesselsConfig;
import anightdazingzoroark.riftedvessels.server.ServerProxy;
import anightdazingzoroark.riftedvessels.server.message.ShipControlStateMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3dc;
import valkyrienwarfare.api.*;
import valkyrienwarfare.api.forces.IShipForces;
import valkyrienwarfare.api.forces.IShipForcesInducer;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns all ship-wide Helm state and applies one set of forces per ship.
 * */
public class ShipHelmController implements IShipForcesInducer, IShipMaintenanceProvider {
    private static final String CONTROL_SESSION_KEY = "RiftedVesselsShipHelmSession";
    private static final String CONTROL_FORWARD_KEY = "RiftedVesselsShipHelmForward";
    private static final String CONTROL_TURN_KEY = "RiftedVesselsShipHelmTurn";
    private static final String CONTROL_VERTICAL_KEY = "RiftedVesselsShipHelmVertical";

    //reduce a specific amnt of velocity per second to make ship stopping smoother
    private static final double RELEASED_VELOCITY_RETENTION_PER_SECOND = 0.5D;
    private static final double RELEASED_FORWARD_VELOCITY_STOP_THRESHOLD = 0.01D;
    private static final double RELEASED_TURN_VELOCITY_STOP_THRESHOLD = 0.001D;
    private static final Vec3d WORLD_UP = new Vec3d(0D, 1D, 0D);
    private static final double MIN_STABILIZATION_ERROR = 1E-6D;
    //increase this value to make ship more stable
    private static final double STABILIZATION_RESPONSE = 90D;
    private static final double STABILIZATION_DAMPING_RATIO = 1D;
    private static final double MAXIMUM_STABILIZATION_TORQUE = 15_000_000D;

    private static final Map<World, Map<UUID, ShipHelmController>> CONTROLLERS_BY_WORLD = Collections.synchronizedMap(new WeakHashMap<>());

    private final World world;
    private final UUID shipUuid;
    private final Set<ShipHelmTileEntity> helms;

    @Nullable
    private IPhysicsEntity ship; //the ship this is attached to
    @Nullable
    private ShipHelmTileEntity controllingHelm;
    @Nullable
    private UUID controllingPlayerUuid;
    @Nullable
    private UUID controlSessionUuid;

    private long lastServerTick = Long.MIN_VALUE;
    private volatile boolean attached;
    private volatile boolean aligning;
    private volatile boolean cruiseControlling;

    //volatility for making a released key visible to the physics callback without waiting for another synchronization point
    private volatile int forwardInput;
    private volatile int turnInput;
    private volatile int verticalInput;

    //volatility for preventing force calculations from retaining an old value
    private volatile int forwardX;
    private volatile int forwardZ;
    private volatile int balloonCount;

    private int lastPhysicsForwardInput;
    private int lastPhysicsTurnInput;
    private boolean slowingForwardMotion;
    private boolean slowingTurningMotion;

    private ShipHelmController(@NotNull IPhysicsEntity ship) {
        this.world = ship.getWorld();
        this.shipUuid = ship.getShipData().getUuid();
        this.helms = ConcurrentHashMap.newKeySet();
        this.attachToShip(ship);
    }

    //---static helpers---
    public static synchronized ShipHelmController attach(@NotNull IPhysicsEntity ship, @NotNull ShipHelmTileEntity helm) {
        Map<UUID, ShipHelmController> worldControllers = CONTROLLERS_BY_WORLD.computeIfAbsent(
                ship.getWorld(), ignored -> new ConcurrentHashMap<>()
        );
        UUID shipUuid = ship.getShipData().getUuid();
        ShipHelmController controller = worldControllers.computeIfAbsent(shipUuid, ignored -> new ShipHelmController(ship));
        controller.attachToShip(ship);
        controller.helms.add(helm);
        controller.synchronizeHelm(helm);
        return controller;
    }

    public static synchronized void release(@NotNull ShipHelmController controller, @NotNull ShipHelmTileEntity helm) {
        controller.removeHelm(helm);
        if (!controller.helms.isEmpty()) return;

        controller.detachFromShip();
        Map<UUID, ShipHelmController> worldControllers = CONTROLLERS_BY_WORLD.get(controller.world);
        if (worldControllers == null) return;
        worldControllers.remove(controller.shipUuid, controller);
        if (worldControllers.isEmpty()) CONTROLLERS_BY_WORLD.remove(controller.world);
    }

    public static void handleControlInput(@NotNull EntityPlayerMP player, int forward, int turn, int vertical, boolean stopControlling) {
        NBTTagCompound controlData = player.getEntityData();
        if (!controlData.hasUniqueId(CONTROL_SESSION_KEY)) {
            ServerProxy.MESSAGE_WRAPPER.sendTo(new ShipControlStateMessage(null), player);
        }
        else if (stopControlling) {
            clearControlSession(player);
            ServerProxy.MESSAGE_WRAPPER.sendTo(new ShipControlStateMessage(null), player);
        }
        else {
            controlData.setInteger(CONTROL_FORWARD_KEY, Math.clamp(forward, -1, 1));
            controlData.setInteger(CONTROL_TURN_KEY, Math.clamp(turn, -1, 1));
            controlData.setInteger(CONTROL_VERTICAL_KEY, Math.clamp(vertical, -1, 1));
        }
    }

    //---internal methods---
    private void attachToShip(@NotNull IPhysicsEntity ship) {
        if (this.ship == ship && this.attached) return;
        this.detachFromShip();
        this.ship = ship;
        this.attached = true;
        this.aligning = ship.isShipAligningToGrid();
        ship.addForceInducer(this);
        ship.addMaintenanceProvider(this);
    }

    private void detachFromShip() {
        this.attached = false;
        this.stopControlling();
        IPhysicsEntity previousShip = this.ship;
        this.ship = null;
        if (previousShip == null) return;
        previousShip.removeForceInducer(this);
        previousShip.removeMaintenanceProvider(this);
    }

    private void removeHelm(@NotNull ShipHelmTileEntity helm) {
        this.helms.remove(helm);
        if (this.controllingHelm == helm) this.stopControlling();
    }

    public boolean manages(@NotNull IPhysicsEntity ship) {
        return this.ship == ship && this.attached;
    }

    @Nullable
    public IPhysicsEntity getShip() {
        return this.ship;
    }

    public void tick() {
        if (!this.attached || this.ship == null || this.world.isRemote) return;
        long serverTick = this.world.getTotalWorldTime();
        if (this.lastServerTick == serverTick) return;
        this.lastServerTick = serverTick;

        this.balloonCount = BalloonController.getBalloonCount(this.ship);

        boolean shipAligning = this.ship.isShipAligningToGrid();
        if (shipAligning != this.aligning) {
            this.aligning = shipAligning;
            if (shipAligning) {
                this.cruiseControlling = false;
                this.stopControlling();
            }
        }
        if (this.ship.isDeconstructing()) {
            this.cruiseControlling = false;
            this.stopControlling();
        }

        this.updateControllingPlayer();
        this.synchronizeHelms();
    }

    //---ship controls---
    //start controlling the ship
    public void startControlling(@NotNull ShipHelmTileEntity helm, @NotNull EntityPlayerMP player) {
        if (!this.attached || this.ship == null || this.aligning || this.ship.isDeconstructing()) return;
        if (!this.helms.contains(helm) || helm.isInvalid()) return;

        UUID playerUuid = player.getUniqueID();
        if (playerUuid.equals(this.controllingPlayerUuid) && this.controllingHelm == helm) {
            if (this.controlSessionUuid != null && hasControlSession(player, this.controlSessionUuid)) {
                ServerProxy.MESSAGE_WRAPPER.sendTo(new ShipControlStateMessage(helm.getPos()), player);
                return;
            }
            this.stopControlling();
        }
        if (this.controllingPlayerUuid != null) return;
        if (!helm.isPlayerStandingOnShip(player) || !helm.isPlayerWithinControlDistance(player, this.ship)) return;

        UUID controlSessionUuid = UUID.randomUUID();
        this.controllingHelm = helm;
        this.controllingPlayerUuid = playerUuid;
        this.controlSessionUuid = controlSessionUuid;
        setControlSession(player, controlSessionUuid);
        if (!this.cruiseControlling) this.captureDirection(helm);

        Vec3d globalForward = this.ship.rotateVector(
                new Vec3d(this.forwardX, 0D, this.forwardZ),
                TransformType.SUBSPACE_TO_GLOBAL
        );
        float yaw = (float) Math.toDegrees(Math.atan2(-globalForward.x, globalForward.z));
        player.rotationYaw = yaw;
        player.setRotationYawHead(yaw);
        ServerProxy.MESSAGE_WRAPPER.sendTo(new ShipControlStateMessage(helm.getPos()), player);
    }

    public void stopControlling(@NotNull ShipHelmTileEntity helm) {
        if (this.controllingHelm == helm) this.stopControlling();
        else helm.setTurning(0);
    }

    private void stopControlling() {
        ShipHelmTileEntity previousHelm = this.controllingHelm;
        UUID playerUuid = this.controllingPlayerUuid;
        UUID sessionUuid = this.controlSessionUuid;
        this.controllingHelm = null;
        this.controllingPlayerUuid = null;
        this.controlSessionUuid = null;
        this.forwardInput = 0;
        this.turnInput = 0;
        this.verticalInput = 0;
        if (previousHelm != null) previousHelm.setTurning(0);

        if (playerUuid == null || sessionUuid == null) return;
        if (!(this.world instanceof WorldServer serverWorld) || serverWorld.getMinecraftServer() == null) return;
        EntityPlayerMP player = serverWorld.getMinecraftServer().getPlayerList().getPlayerByUUID(playerUuid);
        if (player != null && hasControlSession(player, sessionUuid)) {
            clearControlSession(player);
            ServerProxy.MESSAGE_WRAPPER.sendTo(new ShipControlStateMessage(null), player);
        }
    }

    private void updateControllingPlayer() {
        if (this.controllingPlayerUuid == null || this.controlSessionUuid == null || this.controllingHelm == null) {
            this.forwardInput = 0;
            this.turnInput = 0;
            this.verticalInput = 0;
            return;
        }
        if (!(this.world instanceof WorldServer serverWorld) || serverWorld.getMinecraftServer() == null) {
            this.stopControlling();
            return;
        }

        EntityPlayerMP player = serverWorld.getMinecraftServer().getPlayerList().getPlayerByUUID(this.controllingPlayerUuid);
        if (player == null || player.isDead || player.world != this.world
                || !hasControlSession(player, this.controlSessionUuid)
                || this.controllingHelm.isInvalid()
                || this.ship == null
                || !this.controllingHelm.isPlayerWithinControlDistance(player, this.ship)
        ) {
            this.stopControlling();
            return;
        }

        NBTTagCompound controlData = player.getEntityData();
        this.forwardInput = this.cruiseControlling ? 0 : Math.clamp(controlData.getInteger(CONTROL_FORWARD_KEY), -1, 1);
        this.turnInput = Math.clamp(controlData.getInteger(CONTROL_TURN_KEY), -1, 1);
        this.verticalInput = Math.clamp(controlData.getInteger(CONTROL_VERTICAL_KEY), -1, 1);
        this.controllingHelm.setTurning(this.turnInput);
        if (!this.cruiseControlling) this.captureDirection(this.controllingHelm);
    }

    public boolean isBeingControlled() {
        return this.controllingPlayerUuid != null;
    }

    public boolean isControlledBy(@NotNull ShipHelmTileEntity helm, @NotNull EntityPlayerMP player) {
        return this.controllingHelm == helm && player.getUniqueID().equals(this.controllingPlayerUuid);
    }

    public void requestAligningChange(boolean value) {
        if (!this.attached || this.ship == null) return;
        if (this.ship.isDeconstructing()) return;
        if (value && this.cruiseControlling) return;
        this.aligning = value;
        this.ship.setShipAligningToGrid(value);
        if (value) this.stopControlling();
        this.synchronizeHelms();
    }

    public void requestCruiseControlChange(@NotNull ShipHelmTileEntity helm, boolean value) {
        if (!this.attached || this.ship == null || !this.helms.contains(helm)) return;
        if (value && (this.aligning || this.ship.isDeconstructing())) return;
        if (value) this.captureDirection(helm);
        this.cruiseControlling = value;
        if (value) this.forwardInput = 0;
        this.synchronizeHelms();
    }

    public void requestDeconstruction() {
        if (!this.attached || this.ship == null) return;
        this.cruiseControlling = false;
        this.stopControlling();
        this.aligning = true;
        this.ship.setShipAligningToGrid(true);
        this.ship.setDeconstruct(true);
        this.synchronizeHelms();
    }

    private void captureDirection(@NotNull ShipHelmTileEntity helm) {
        EnumFacing helmFacing = helm.getFacing();
        this.forwardX = helmFacing == null ? 0 : -helmFacing.getXOffset();
        this.forwardZ = helmFacing == null ? 0 : -helmFacing.getZOffset();
    }

    private void synchronizeHelms() {
        for (ShipHelmTileEntity helm : this.helms) this.synchronizeHelm(helm);
    }

    private void synchronizeHelm(@NotNull ShipHelmTileEntity helm) {
        if (helm.isInvalid()) return;
        if (!helm.isAssembled()) helm.setAssembled(true);
        if (helm.getAligning() != this.aligning) helm.setAligning(this.aligning);
        if (helm.getCruiseControlling() != this.cruiseControlling) {
            helm.setCruiseControlling(this.cruiseControlling);
        }
        if (helm != this.controllingHelm) helm.setTurning(0);
    }

    @Override
    public boolean canMaintainShip() {
        if (!this.attached || this.ship == null) return false;
        for (ShipHelmTileEntity helm : this.helms) {
            if (!helm.isInvalid()) return true;
        }
        return false;
    }

    //---forces on the ship---
    @Override
    public void applyForces(@NotNull IShipForces forces) {
        IPhysicsEntity forceShip = forces.getShip();
        IShipData shipData = forceShip.getShipData();
        if (!this.attached || this.ship != forceShip || this.aligning
                || forceShip.isShipAligningToGrid() || forceShip.isDeconstructing()
                || !shipData.isPhysicsEnabled()
        ) {
            this.resetPhysicsInputState();
            return;
        }

        double mass = shipData.getMass();
        if (mass <= 0D) return;
        this.applyStabilizingTorque(forces, shipData, mass);

        int forward = this.cruiseControlling ? 1 : this.forwardInput;
        int turn = this.turnInput;
        int vertical = this.verticalInput;

        if (this.balloonCount >= RiftedVesselsConfig.shipBalloonMinimumCount) {
            this.applyVerticalForce(forces, shipData, mass, vertical);
        }

        //no forward/backward input means slowing down to smoothen travel
        if (forward == 0 && this.lastPhysicsForwardInput != 0) this.slowingForwardMotion = true;
        else if (forward != 0) this.slowingForwardMotion = false;

        //same for no turning input
        if (turn == 0 && this.lastPhysicsTurnInput != 0) this.slowingTurningMotion = true;
        else if (turn != 0) this.slowingTurningMotion = false;

        //block further forces when there's no more input
        this.lastPhysicsForwardInput = forward;
        this.lastPhysicsTurnInput = turn;
        if (forward == 0 && turn == 0 && !this.slowingForwardMotion && !this.slowingTurningMotion) return;

        //apply forward/backward motion
        if (this.slowingForwardMotion) {
            this.slowingForwardMotion = this.slowForwardMomentum(forces, shipData, mass);
        }
        if (forward != 0) this.applyForwardForce(forces, shipData, mass, forward);

        //apply left/right rotation
        if (this.slowingTurningMotion) {
            this.slowingTurningMotion = this.slowTurningMomentum(forces, shipData, mass);
        }
        if (turn != 0) this.applyTurningTorque(forces, shipData, mass, turn);
    }

    private void resetPhysicsInputState() {
        this.lastPhysicsForwardInput = 0;
        this.lastPhysicsTurnInput = 0;
        this.slowingForwardMotion = false;
        this.slowingTurningMotion = false;
    }

    //as a simple ship mod, a stabilizing torque is necessary to reduce headaches involving construction
    private void applyStabilizingTorque(@NotNull IShipForces forces, @NotNull IShipData shipData, double mass) {
        IPhysicsEntity ship = forces.getShip();
        Vec3d shipUp = ship.rotateVector(WORLD_UP, TransformType.SUBSPACE_TO_GLOBAL).normalize();
        Vec3d correctionAxis = shipUp.crossProduct(WORLD_UP);
        double correctionAxisLengthSquared = correctionAxis.dotProduct(correctionAxis);
        Vec3d correctionAcceleration = new Vec3d(0D, 0D, 0D);
        if (correctionAxisLengthSquared >= MIN_STABILIZATION_ERROR) {
            correctionAxis = correctionAxis.scale(1D / Math.sqrt(correctionAxisLengthSquared));
            double angle = Math.acos(Math.clamp(shipUp.dotProduct(WORLD_UP), -1D, 1D));
            correctionAcceleration = correctionAxis.scale(angle * STABILIZATION_RESPONSE);
        }

        Vector3dc angularVelocity = shipData.getAngularVelocity();
        Vec3d currentAngularVelocity = new Vec3d(angularVelocity.x(), angularVelocity.y(), angularVelocity.z());
        Vec3d levelingAngularVelocity = currentAngularVelocity.subtract(
                WORLD_UP.scale(currentAngularVelocity.dotProduct(WORLD_UP))
        );
        double stabilizationDamping = 2D * Math.sqrt(STABILIZATION_RESPONSE) * STABILIZATION_DAMPING_RATIO;
        Vec3d angularVelocityChange = correctionAcceleration.subtract(
                levelingAngularVelocity.scale(stabilizationDamping)
        );

        Vec3d shipAngularVelocityChange = ship.rotateVector(
                angularVelocityChange, TransformType.GLOBAL_TO_SUBSPACE
        );
        Vec3d momentsOfInertia = this.getMomentsOfInertia(shipData, mass);
        Vec3d shipTorque = new Vec3d(
                shipAngularVelocityChange.x * momentsOfInertia.x,
                shipAngularVelocityChange.y * momentsOfInertia.y,
                shipAngularVelocityChange.z * momentsOfInertia.z
        );
        Vec3d torque = ship.rotateVector(shipTorque, TransformType.SUBSPACE_TO_GLOBAL);
        torque = torque.subtract(WORLD_UP.scale(torque.dotProduct(WORLD_UP)));

        double torqueMagnitude = Math.sqrt(torque.dotProduct(torque));
        if (torqueMagnitude > MAXIMUM_STABILIZATION_TORQUE) {
            torque = torque.scale(MAXIMUM_STABILIZATION_TORQUE / torqueMagnitude);
        }
        forces.applyWorldTorque(torque);
    }

    //releasing forward/backward buttons must cause an immediate slowdown
    private boolean slowForwardMomentum(@NotNull IShipForces forces, @NotNull IShipData shipData, double mass) {
        double timeStep = forces.getTimeStepSeconds();
        if (timeStep <= 0D) return true;

        Vector3dc linearVelocity = shipData.getLinearVelocity();
        Vec3d shipVelocity = forces.getShip().rotateVector(
                new Vec3d(linearVelocity.x(), linearVelocity.y(), linearVelocity.z()),
                TransformType.GLOBAL_TO_SUBSPACE
        );
        double forwardVelocity = shipVelocity.x * this.forwardX + shipVelocity.z * this.forwardZ;
        if (Math.abs(forwardVelocity) <= RELEASED_FORWARD_VELOCITY_STOP_THRESHOLD) return false;

        double retainedVelocity = Math.pow(RELEASED_VELOCITY_RETENTION_PER_SECOND, timeStep);
        double cancellingForce = mass * forwardVelocity * (retainedVelocity - 1D) / timeStep;
        forces.applyShipForce(new Vec3d(
                this.forwardX * cancellingForce,
                0D,
                this.forwardZ * cancellingForce
        ));
        return true;
    }

    //releasing left/right buttons must cause an immediate slowdown
    private boolean slowTurningMomentum(@NotNull IShipForces forces, @NotNull IShipData shipData, double mass) {
        double timeStep = forces.getTimeStepSeconds();
        if (timeStep <= 0D) return true;

        Vector3dc angularVelocity = shipData.getAngularVelocity();
        double yawVelocity = angularVelocity.y();
        if (Math.abs(yawVelocity) <= RELEASED_TURN_VELOCITY_STOP_THRESHOLD) return false;

        double yawMomentOfInertia = this.getWorldYawMomentOfInertia(forces.getShip(), shipData, mass);
        double retainedVelocity = Math.pow(RELEASED_VELOCITY_RETENTION_PER_SECOND, timeStep);
        double cancellingTorque = yawMomentOfInertia * yawVelocity * (retainedVelocity - 1D) / timeStep;
        forces.applyWorldTorque(new Vec3d(0D, cancellingTorque, 0D));
        return true;
    }

    //apply forward/backward ship movement
    private void applyForwardForce(@NotNull IShipForces forces, @NotNull IShipData shipData, double mass, int forward) {
        double directionScale = forward < 0 ? 0.25D : 1D;
        double forceX = forward * this.forwardX * directionScale; //slow down when going backwards
        double forceZ = forward * this.forwardZ * directionScale; //same as above
        Vector3dc linearVelocity = shipData.getLinearVelocity();
        Vec3d shipVelocity = forces.getShip().rotateVector(
                new Vec3d(linearVelocity.x(), linearVelocity.y(), linearVelocity.z()),
                TransformType.GLOBAL_TO_SUBSPACE
        );
        double velocityInRequestedDirection = shipVelocity.x * forceX + shipVelocity.z * forceZ;
        if (velocityInRequestedDirection >= RiftedVesselsConfig.shipHelmMaximumSpeed) return;

        double forceMagnitude = mass * RiftedVesselsConfig.shipHelmAcceleration;
        forces.applyShipForce(new Vec3d(forceX * forceMagnitude, 0D, forceZ * forceMagnitude));
    }

    //apply left/right ship turning
    private void applyTurningTorque(@NotNull IShipForces forces, @NotNull IShipData shipData, double mass, int turn) {
        Vector3dc angularVelocity = shipData.getAngularVelocity();
        double maximumTurnSpeedRadians = Math.toRadians(RiftedVesselsConfig.shipHelmMaximumTurnSpeed);
        if (angularVelocity.y() * turn >= maximumTurnSpeedRadians) return;

        double yawMomentOfInertia = this.getWorldYawMomentOfInertia(forces.getShip(), shipData, mass);
        double torque = yawMomentOfInertia * Math.toRadians(RiftedVesselsConfig.shipHelmTurnAcceleration) * turn;
        forces.applyWorldTorque(new Vec3d(0D, torque, 0D));
    }

    private void applyVerticalForce(@NotNull IShipForces forces, @NotNull IShipData shipData, double mass, int vertical) {
        double timeStep = forces.getTimeStepSeconds();
        if (timeStep <= 0D || RiftedVesselsConfig.shipBalloonVerticalAcceleration <= 0D) return;

        double targetVelocity;
        if (vertical > 0) targetVelocity = RiftedVesselsConfig.shipBalloonMaximumAscentSpeed;
        else if (vertical < 0) targetVelocity = -RiftedVesselsConfig.shipBalloonMaximumDescentSpeed;
        else targetVelocity = 0D;

        double currentVelocity = shipData.getLinearVelocity().y();
        double desiredAcceleration = Math.clamp(
                (targetVelocity - currentVelocity) / timeStep,
                -RiftedVesselsConfig.shipBalloonVerticalAcceleration,
                RiftedVesselsConfig.shipBalloonVerticalAcceleration
        );
        double force = mass * (desiredAcceleration - RiftedValkyriumAPI.getWorldGravity().y());
        if (force <= 0D) return;
        forces.applyWorldForce(new Vec3d(0D, force, 0D));
    }

    private double getWorldYawMomentOfInertia(@NotNull IPhysicsEntity ship, @NotNull IShipData shipData, double mass) {
        Vec3d worldUpInShip = ship.rotateVector(WORLD_UP, TransformType.GLOBAL_TO_SUBSPACE);
        Vec3d momentsOfInertia = this.getMomentsOfInertia(shipData, mass);
        return momentsOfInertia.x * worldUpInShip.x * worldUpInShip.x
                + momentsOfInertia.y * worldUpInShip.y * worldUpInShip.y
                + momentsOfInertia.z * worldUpInShip.z * worldUpInShip.z;
    }

    private Vec3d getMomentsOfInertia(@NotNull IShipData shipData, double mass) {
        AxisAlignedBB shipBounds = shipData.getShipBB();
        double width = Math.max(shipBounds.maxX - shipBounds.minX, 1D);
        double height = Math.max(shipBounds.maxY - shipBounds.minY, 1D);
        double depth = Math.max(shipBounds.maxZ - shipBounds.minZ, 1D);
        return new Vec3d(
                mass * (height * height + depth * depth) / 12D,
                mass * (width * width + depth * depth) / 12D,
                mass * (width * width + height * height) / 12D
        );
    }

    //---setting control session for player---
    private static void setControlSession(@NotNull EntityPlayerMP player, @NotNull UUID controlSessionUuid) {
        NBTTagCompound controlData = player.getEntityData();
        controlData.setUniqueId(CONTROL_SESSION_KEY, controlSessionUuid);
        controlData.setInteger(CONTROL_FORWARD_KEY, 0);
        controlData.setInteger(CONTROL_TURN_KEY, 0);
        controlData.setInteger(CONTROL_VERTICAL_KEY, 0);
    }

    private static boolean hasControlSession(@NotNull EntityPlayerMP player, @NotNull UUID controlSessionUuid) {
        NBTTagCompound controlData = player.getEntityData();
        return controlData.hasUniqueId(CONTROL_SESSION_KEY) && controlSessionUuid.equals(controlData.getUniqueId(CONTROL_SESSION_KEY));
    }

    private static void clearControlSession(@NotNull EntityPlayerMP player) {
        NBTTagCompound controlData = player.getEntityData();
        controlData.removeTag(CONTROL_SESSION_KEY + "Most");
        controlData.removeTag(CONTROL_SESSION_KEY + "Least");
        controlData.removeTag(CONTROL_FORWARD_KEY);
        controlData.removeTag(CONTROL_TURN_KEY);
        controlData.removeTag(CONTROL_VERTICAL_KEY);
    }
}
