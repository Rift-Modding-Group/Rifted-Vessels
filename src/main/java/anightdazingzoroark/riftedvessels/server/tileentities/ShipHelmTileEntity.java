package anightdazingzoroark.riftedvessels.server.tileentities;

import anightdazingzoroark.riftedvessels.RiftedVesselsConfig;
import anightdazingzoroark.riftedvessels.server.RiftedVesselsUtil;
import anightdazingzoroark.riftedvessels.server.ServerProxy;
import anightdazingzoroark.riftedvessels.server.blocks.ShipHelmBlock;
import anightdazingzoroark.riftedvessels.server.message.ShipControlStateMessage;
import anightdazingzoroark.riftlib.nbtStorageUser.propertyValue.BooleanPropertyValue;
import anightdazingzoroark.riftlib.nbtStorageUser.propertyValue.IntegerPropertyValue;
import anightdazingzoroark.riftlib.nbtStorageUser.tileEntity.RiftTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3dc;
import valkyrienwarfare.api.IPhysicsEntity;
import valkyrienwarfare.api.IShipData;
import valkyrienwarfare.api.RiftedValkyriumAPI;
import valkyrienwarfare.api.TransformType;
import valkyrienwarfare.api.forces.IShipForces;
import valkyrienwarfare.api.forces.ShipForcesInducer;

import java.util.Optional;
import java.util.UUID;

public class ShipHelmTileEntity extends RiftTileEntity implements ITickable, ShipForcesInducer {
    private static final String CONTROL_SESSION_KEY = "RiftedVesselsShipHelmSession";
    private static final String CONTROL_FORWARD_KEY = "RiftedVesselsShipHelmForward";
    private static final String CONTROL_TURN_KEY = "RiftedVesselsShipHelmTurn";
    private static final double MAXIMUM_CONTROL_DISTANCE = 3D;

    //reduce a specific amnt of velocity per second to make ship stopping smoother
    private static final double RELEASED_VELOCITY_RETENTION_PER_SECOND = 0.5D;
    private static final double RELEASED_FORWARD_VELOCITY_STOP_THRESHOLD = 0.01D;
    private static final double RELEASED_TURN_VELOCITY_STOP_THRESHOLD = 0.001D;
    private static final Vec3d WORLD_UP = new Vec3d(0D, 1D, 0D);
    private static final double MIN_STABILIZATION_ERROR = 1.0E-6D;
    private static final double STABILIZATION_RESPONSE = 45D;
    private static final double STABILIZATION_DAMPING_RATIO = 1D;
    private static final double MAXIMUM_STABILIZATION_TORQUE = 15_000_000D;

    @Nullable
    private UUID controllingPlayerUuid;
    @Nullable
    private UUID controlSessionUuid;
    @Nullable
    private IPhysicsEntity inducedShip; //the ship this is attached to

    //volatility for making a released key visible to the physics callback without waiting for another synchronization point
    private volatile int forwardInput;
    private volatile int turnInput;

    //volatility for preventing the force calculation from retaining an old facing or config value
    private volatile int forwardX;
    private volatile int forwardZ;
    private volatile double acceleration = RiftedVesselsConfig.shipHelmAcceleration;
    private volatile double maximumSpeed = RiftedVesselsConfig.shipHelmMaximumSpeed;
    private volatile double turnAcceleration = RiftedVesselsConfig.shipHelmTurnAcceleration;
    private volatile double maximumTurnSpeed = RiftedVesselsConfig.shipHelmMaximumTurnSpeed;
    private int lastPhysicsForwardInput;
    private int lastPhysicsTurnInput;
    private boolean slowingForwardMotion;
    private boolean slowingTurningMotion;

    @Override
    public void registerValues() {
        this.registerValue(new BooleanPropertyValue("Assembled", false));
        this.registerValue(new BooleanPropertyValue("Aligning", false));
        this.registerValue(new BooleanPropertyValue("CruiseControlling", false));
        this.registerValue(new IntegerPropertyValue("Turning", 0));
    }

    @Override
    public void update() {
        if (this.world.isRemote) return;

        //the ship cos... well...
        Optional<IPhysicsEntity> ship = RiftedValkyriumAPI.getPhysoManagingBlock(this.world, this.pos);

        //set assembled
        if (ship.isEmpty() && this.isAssembled()) {
            IShipData shipData = RiftedValkyriumAPI.createNewShip((WorldServer) this.world, this.pos, RiftedVesselsUtil.getBlockBlacklist());
            shipData.setPhysicsEnabled(true);
        }

        //set induced ship, aligning to grid, and cruise control
        if (ship.isPresent() && this.isAssembled()) {
            this.setInducedShip(ship.get());
            ship.get().setShipAligningToGrid(this.getAligning());
            this.updateControllingPlayer(ship.get());
        }

        //set ship disassembled
        if (ship.isPresent() && !this.isAssembled()) {
            this.stopControlling();
            this.clearInducedShip();
            ship.get().setShipAligningToGrid(true);
            ship.get().setDeconstruct(true);
        }
    }

    //---ship controls---
    //start controlling the ship
    public void startControlling(@NotNull EntityPlayerMP player) {
        if (this.world.isRemote || !this.isAssembled()) return;

        UUID playerUuid = player.getUniqueID();
        if (playerUuid.equals(this.controllingPlayerUuid)) {
            if (this.controlSessionUuid != null && hasControlSession(player, this.controlSessionUuid)) {
                ServerProxy.MESSAGE_WRAPPER.sendTo(new ShipControlStateMessage(true), player);
                return;
            }
            this.stopControlling();
        }
        if (this.controllingPlayerUuid != null) return;

        Optional<IPhysicsEntity> ship = RiftedValkyriumAPI.getPhysoManagingBlock(this.world, this.pos);
        if (ship.isEmpty() || !this.isPlayerStandingOnShip(player) || !this.isPlayerWithinControlDistance(player, ship.get())) return;

        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof ShipHelmBlock)) return;
        EnumFacing facing = state.getValue(ShipHelmBlock.FACING);

        UUID controlSessionUuid = UUID.randomUUID();
        this.controllingPlayerUuid = playerUuid;
        this.controlSessionUuid = controlSessionUuid;
        setControlSession(player, controlSessionUuid);
        this.forwardX = -facing.getXOffset();
        this.forwardZ = -facing.getZOffset();
        this.acceleration = RiftedVesselsConfig.shipHelmAcceleration;
        this.maximumSpeed = RiftedVesselsConfig.shipHelmMaximumSpeed;
        this.turnAcceleration = RiftedVesselsConfig.shipHelmTurnAcceleration;
        this.maximumTurnSpeed = RiftedVesselsConfig.shipHelmMaximumTurnSpeed;
        this.setInducedShip(ship.get());

        Vec3d globalForward = ship.get().rotateVector(
                new Vec3d(this.forwardX, 0D, this.forwardZ),
                TransformType.SUBSPACE_TO_GLOBAL
        );
        float yaw = (float) Math.toDegrees(Math.atan2(-globalForward.x, globalForward.z));
        player.rotationYaw = yaw;
        player.setRotationYawHead(yaw);
        ServerProxy.MESSAGE_WRAPPER.sendTo(new ShipControlStateMessage(true), player);
    }

    public static void handleControlInput(@NotNull EntityPlayerMP player, int forward, int turn, boolean stopControlling) {
        NBTTagCompound controlData = player.getEntityData();
        if (!controlData.hasUniqueId(CONTROL_SESSION_KEY)) {
            ServerProxy.MESSAGE_WRAPPER.sendTo(new ShipControlStateMessage(false), player);
        }
        else if (stopControlling) {
            clearControlSession(player);
            ServerProxy.MESSAGE_WRAPPER.sendTo(new ShipControlStateMessage(false), player);
        }
        else {
            controlData.setInteger(CONTROL_FORWARD_KEY, Math.clamp(forward, -1, 1));
            controlData.setInteger(CONTROL_TURN_KEY, Math.clamp(turn, -1, 1));
        }
    }

    private void updateControllingPlayer(@NotNull IPhysicsEntity ship) {
        if (this.controllingPlayerUuid == null || this.controlSessionUuid == null) return;
        if (!(this.world instanceof WorldServer serverWorld) || serverWorld.getMinecraftServer() == null) {
            this.stopControlling();
            return;
        }

        EntityPlayerMP player = serverWorld.getMinecraftServer().getPlayerList().getPlayerByUUID(this.controllingPlayerUuid);
        if (player == null || player.isDead || player.world != this.world || !hasControlSession(player, this.controlSessionUuid)) {
            this.stopControlling();
            return;
        }
        if (!this.isPlayerWithinControlDistance(player, ship)) {
            this.stopControlling();
            return;
        }

        NBTTagCompound controlData = player.getEntityData();
        this.forwardInput = Math.clamp(controlData.getInteger(CONTROL_FORWARD_KEY), -1, 1);
        this.turnInput = Math.clamp(controlData.getInteger(CONTROL_TURN_KEY), -1, 1);
        this.setTurning(this.turnInput);
        this.acceleration = RiftedVesselsConfig.shipHelmAcceleration;
        this.maximumSpeed = RiftedVesselsConfig.shipHelmMaximumSpeed;
        this.turnAcceleration = RiftedVesselsConfig.shipHelmTurnAcceleration;
        this.maximumTurnSpeed = RiftedVesselsConfig.shipHelmMaximumTurnSpeed;
        if (this.inducedShip != ship) this.setInducedShip(ship);
    }

    private void setInducedShip(@NotNull IPhysicsEntity ship) {
        if (this.inducedShip == ship) return;
        this.clearInducedShip();
        this.inducedShip = ship;
        ship.addForceInducer(this);
    }

    private void clearInducedShip() {
        if (this.inducedShip != null) this.inducedShip.removeForceInducer(this);
        this.inducedShip = null;
    }

    public void stopControlling() {
        this.forwardInput = 0;
        this.turnInput = 0;
        this.setTurning(0);
        UUID playerUuid = this.controllingPlayerUuid;
        UUID controlSessionUuid = this.controlSessionUuid;
        this.controllingPlayerUuid = null;
        this.controlSessionUuid = null;

        if (playerUuid != null && controlSessionUuid != null) {
            if (this.world instanceof WorldServer serverWorld && serverWorld.getMinecraftServer() != null) {
                EntityPlayerMP player = serverWorld.getMinecraftServer().getPlayerList().getPlayerByUUID(playerUuid);
                if (player != null && hasControlSession(player, controlSessionUuid)) {
                    clearControlSession(player);
                    ServerProxy.MESSAGE_WRAPPER.sendTo(new ShipControlStateMessage(false), player);
                }
            }
        }

    }

    private static void setControlSession(@NotNull EntityPlayerMP player, @NotNull UUID controlSessionUuid) {
        NBTTagCompound controlData = player.getEntityData();
        controlData.setUniqueId(CONTROL_SESSION_KEY, controlSessionUuid);
        controlData.setInteger(CONTROL_FORWARD_KEY, 0);
        controlData.setInteger(CONTROL_TURN_KEY, 0);
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
    }

    public boolean isBeingControlled() {
        return this.controllingPlayerUuid != null;
    }

    public boolean isControlledBy(@NotNull EntityPlayerMP player) {
        return player.getUniqueID().equals(this.controllingPlayerUuid);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isPlayerStandingOnShip(@NotNull EntityPlayerMP player) {
        if (this.world.isRemote || player.world != this.world) return false;
        if (!RiftedValkyriumAPI.isPosInShipyard(this.world, player.getPosition().down())) return false;

        IShipData helmShipData = this.inducedShip != null ? this.inducedShip.getShipData() : null;
        Optional<IShipData> playerShip = RiftedValkyriumAPI.getActiveShipOfEntity(player);
        return helmShipData != null && playerShip.isPresent() && helmShipData.getUuid().equals(playerShip.get().getUuid());
    }

    public boolean isPlayerWithinControlDistance(@NotNull EntityPlayerMP player) {
        IPhysicsEntity helmShip = this.inducedShip != null ? this.inducedShip : null;
        return helmShip != null && this.isPlayerWithinControlDistance(player, helmShip);
    }

    private boolean isPlayerWithinControlDistance(@NotNull EntityPlayerMP player, @NotNull IPhysicsEntity ship) {
        if (player.world != this.world) return false;

        Vec3d helmCenter = new Vec3d(
                this.pos.getX() + 0.5D,
                this.pos.getY() + 0.5D,
                this.pos.getZ() + 0.5D
        );
        if (!RiftedValkyriumAPI.isPosInShipyard(this.world, player.getPosition())) {
            helmCenter = ship.transformVector(helmCenter, TransformType.SUBSPACE_TO_GLOBAL);
        }
        return player.getDistanceSq(helmCenter.x, helmCenter.y, helmCenter.z) <= MAXIMUM_CONTROL_DISTANCE * MAXIMUM_CONTROL_DISTANCE;
    }

    //---forces on the ship---
    @Override
    public void applyForces(@NotNull IShipForces forces) {
        IShipData shipData = forces.getShip().getShipData();
        double mass = shipData.getMass();
        if (mass <= 0D) return;

        this.applyStabilizingTorque(forces, shipData, mass);

        int forward = this.forwardInput;
        int turn = this.turnInput;

        //no forward/backward input means slowing down to smoothen travel
        if (forward == 0 && this.lastPhysicsForwardInput != 0) this.slowingForwardMotion = true;
        else if (forward != 0) this.slowingForwardMotion = false;

        //same for no turning input
        if (turn == 0 && this.lastPhysicsTurnInput != 0) this.slowingTurningMotion = true;
        else if (turn != 0) this.slowingTurningMotion = false;

        this.lastPhysicsForwardInput = forward;
        this.lastPhysicsTurnInput = turn;
        if (forward == 0 && turn == 0 && !this.slowingForwardMotion && !this.slowingTurningMotion) return;

        double configuredAcceleration = this.acceleration;
        double configuredMaximumSpeed = this.maximumSpeed;

        if (this.slowingForwardMotion) {
            this.slowingForwardMotion = this.slowForwardMomentum(forces, shipData, mass);
        }
        if (forward != 0 && configuredAcceleration > 0D && configuredMaximumSpeed > 0D) {
            this.applyForwardForce(forces, shipData, mass, forward, configuredAcceleration, configuredMaximumSpeed);
        }

        double configuredTurnAcceleration = this.turnAcceleration;
        double configuredMaximumTurnSpeed = this.maximumTurnSpeed;
        if (this.slowingTurningMotion) {
            this.slowingTurningMotion = this.slowTurningMomentum(forces, shipData, mass);
        }
        if (turn != 0 && configuredTurnAcceleration > 0D && configuredMaximumTurnSpeed > 0D) {
            this.applyTurningTorque(forces, shipData, mass, turn, configuredTurnAcceleration, configuredMaximumTurnSpeed);
        }
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
        Vec3d currentAngularVelocity = new Vec3d(
                angularVelocity.x(),
                angularVelocity.y(),
                angularVelocity.z()
        );
        Vec3d levelingAngularVelocity = currentAngularVelocity.subtract(WORLD_UP.scale(currentAngularVelocity.dotProduct(WORLD_UP)));
        double stabilizationDamping = 2D * Math.sqrt(STABILIZATION_RESPONSE) * STABILIZATION_DAMPING_RATIO;
        Vec3d angularVelocityChange = correctionAcceleration.subtract(
                levelingAngularVelocity.scale(stabilizationDamping)
        );

        Vec3d shipAngularVelocityChange = ship.rotateVector(angularVelocityChange, TransformType.GLOBAL_TO_SUBSPACE);
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
    private void applyForwardForce(
            @NotNull IShipForces forces, @NotNull IShipData shipData, double mass,
            int forward, double configuredAcceleration, double configuredMaximumSpeed
    ) {
        double forceX = forward * this.forwardX * (forward < 0 ? 0.25D : 1D); //slow down when going backwards
        double forceZ = forward * this.forwardZ * (forward < 0 ? 0.25D : 1D); //same as above
        Vector3dc linearVelocity = shipData.getLinearVelocity();
        Vec3d shipVelocity = forces.getShip().rotateVector(
                new Vec3d(linearVelocity.x(), linearVelocity.y(), linearVelocity.z()),
                TransformType.GLOBAL_TO_SUBSPACE
        );
        double velocityInRequestedDirection = shipVelocity.x * forceX + shipVelocity.z * forceZ;
        if (velocityInRequestedDirection >= configuredMaximumSpeed) return;

        double forceMagnitude = mass * configuredAcceleration;
        forces.applyShipForce(new Vec3d(forceX * forceMagnitude, 0D, forceZ * forceMagnitude));
    }

    //apply left/right ship turning
    private void applyTurningTorque(
            @NotNull IShipForces forces, @NotNull IShipData shipData, double mass,
            int turn, double configuredTurnAcceleration, double configuredMaximumTurnSpeed
    ) {
        Vector3dc angularVelocity = shipData.getAngularVelocity();
        double maximumTurnSpeedRadians = Math.toRadians(configuredMaximumTurnSpeed);
        if (angularVelocity.y() * turn >= maximumTurnSpeedRadians) return;

        double yawMomentOfInertia = this.getWorldYawMomentOfInertia(forces.getShip(), shipData, mass);
        double torque = yawMomentOfInertia * Math.toRadians(configuredTurnAcceleration) * turn;
        forces.applyWorldTorque(new Vec3d(0D, torque, 0D));
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

    //---global getters and setters---
    public void setAssembled(boolean value) {
        this.setValue("Assembled", value);
        if (!value) {
            this.setCruiseControlling(false);
            this.stopControlling();
            this.clearInducedShip();
        }
    }

    public boolean isAssembled() {
        return this.getValue("Assembled");
    }

    public void setAligning(boolean value) {
        this.setValue("Aligning", value);
    }

    public boolean getAligning() {
        return this.getValue("Aligning");
    }

    public void setCruiseControlling(boolean value) {
        this.setValue("CruiseControlling", false);
    }

    public boolean getCruiseControlling() {
        return this.getValue("CruiseControlling");
    }

    public void setTurning(int value) {
        int turning = Math.clamp(value, -1, 1);
        if (turning != this.getTurning()) this.setValue("Turning", turning);
    }

    public int getTurning() {
        return this.getValue("Turning");
    }

    //---other overrides---
    @Override
    public void invalidate() {
        this.stopControlling();
        this.clearInducedShip();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        this.stopControlling();
        this.clearInducedShip();
        super.onChunkUnload();
    }
}
