package anightdazingzoroark.riftedvessels.server.tileentities;

import anightdazingzoroark.riftedvessels.RiftedVesselsUtil;
import anightdazingzoroark.riftedvessels.server.blocks.ShipHelmBlock;
import anightdazingzoroark.riftlib.nbtStorageUser.propertyValue.BooleanPropertyValue;
import anightdazingzoroark.riftlib.nbtStorageUser.propertyValue.IntegerPropertyValue;
import anightdazingzoroark.riftlib.nbtStorageUser.tileEntity.RiftTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import valkyrienwarfare.api.IPhysicsEntity;
import valkyrienwarfare.api.IShipData;
import valkyrienwarfare.api.RiftedValkyriumAPI;
import valkyrienwarfare.api.TransformType;

import java.util.Optional;

public class ShipHelmTileEntity extends RiftTileEntity implements ITickable {
    private static final double MAXIMUM_CONTROL_DISTANCE = 3D;

    @Nullable
    private ShipHelmController controller;
    private boolean assemblyRequested;
    private boolean disassemblyRequested;

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
        Optional<IPhysicsEntity> optionalShip = RiftedValkyriumAPI.getPhysoManagingBlock(this.world, this.pos);
        if (optionalShip.isEmpty()) {
            //disassemble
            if (this.isAssembled()) this.setAssembled(false);
            //
            else {
                this.clearController();
                if (this.getAligning()) this.setAligning(false);
                if (this.getCruiseControlling()) this.setCruiseControlling(false);
                if (this.getTurning() != 0) this.setTurning(0);
            }
            this.disassemblyRequested = false;

            //assemble
            if (this.assemblyRequested && this.world instanceof WorldServer serverWorld) {
                this.assemblyRequested = false;
                RiftedValkyriumAPI.createNewShip(serverWorld, this.pos, RiftedVesselsUtil.getBlockBlacklist(), true);
            }
        }
        else {
            IPhysicsEntity ship = optionalShip.get();
            this.assemblyRequested = false;

            //set assembled
            if (!this.isAssembled()) this.setAssembled(true);

            //set induced ship, aligning to grid, and cruise control
            this.setControllerShip(ship);
            if (this.controller == null) return;

            this.controller.tick();

            //set ship disassembled
            if (this.disassemblyRequested) {
                this.disassemblyRequested = false;
                this.controller.requestDeconstruction();
            }
        }
    }

    //---these setters may suspiciously look like the ones for syncing, but are instead for requesting state changes---
    public void requestAssemblyChange(boolean assemble) {
        this.assemblyRequested = assemble;
        this.disassemblyRequested = !assemble;
    }

    public void requestAligningChange(boolean align) {
        if (this.world == null || this.world.isRemote || this.controller == null) return;
        this.controller.requestAligningChange(align);
    }

    public void requestCruiseControlChange(boolean cruiseControl) {
        if (this.world == null || this.world.isRemote || this.controller == null) return;
        this.controller.requestCruiseControlChange(this, cruiseControl);
    }
    //------

    private void setControllerShip(@NotNull IPhysicsEntity ship) {
        if (this.controller != null && this.controller.manages(ship)) return;
        this.clearController();
        this.controller = ShipHelmController.attach(ship, this);
    }

    private void clearController() {
        if (this.controller == null) return;
        ShipHelmController previousController = this.controller;
        this.controller = null;
        ShipHelmController.release(previousController, this);
    }

    //---ship controls---
    //start controlling the ship
    public void startControlling(@NotNull EntityPlayerMP player) {
        if (this.world.isRemote || !this.isAssembled() || this.getAligning()) return;
        if (this.controller != null) this.controller.startControlling(this, player);
    }

    public void stopControlling() {
        if (this.controller != null) this.controller.stopControlling(this);
        else this.setTurning(0);
    }

    public boolean isBeingControlled() {
        return this.controller != null && this.controller.isBeingControlled();
    }

    public boolean isControlledBy(@NotNull EntityPlayerMP player) {
        return this.controller != null && this.controller.isControlledBy(this, player);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isPlayerStandingOnShip(@NotNull EntityPlayerMP player) {
        if (this.world.isRemote || player.world != this.world) return false;
        if (!RiftedValkyriumAPI.isPosInShipyard(this.world, player.getPosition().down())) return false;

        IPhysicsEntity ship = this.controller == null ? null : this.controller.getShip();
        IShipData helmShipData = ship == null ? null : ship.getShipData();
        Optional<IShipData> playerShip = RiftedValkyriumAPI.getActiveShipOfEntity(player);
        return helmShipData != null && playerShip.isPresent() && helmShipData.getUuid().equals(playerShip.get().getUuid());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isPlayerWithinControlDistance(@NotNull EntityPlayerMP player) {
        IPhysicsEntity ship = this.controller == null ? null : this.controller.getShip();
        return ship != null && this.isPlayerWithinControlDistance(player, ship);
    }

    public boolean isPlayerWithinControlDistance(@NotNull EntityPlayerMP player, @NotNull IPhysicsEntity ship) {
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

    @Nullable
    public EnumFacing getFacing() {
        if (this.world == null) return null;
        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof ShipHelmBlock)) return null;
        return state.getValue(ShipHelmBlock.FACING);
    }

    //---global getters and setters for storing and synchronizing values---
    protected void setAssembled(boolean value) {
        this.setValue("Assembled", value);
        if (!value) {
            this.setAligning(false);
            this.setCruiseControlling(false);
            this.stopControlling();
            this.clearController();
        }
    }

    public boolean isAssembled() {
        return this.getValue("Assembled");
    }

    public void setAligning(boolean value) {
        this.setValue("Aligning", value);
        this.setCruiseControlling(false);
    }

    public boolean getAligning() {
        return this.getValue("Aligning");
    }

    protected void setCruiseControlling(boolean value) {
        this.setValue("CruiseControlling", value);
    }

    public boolean getCruiseControlling() {
        return this.getValue("CruiseControlling");
    }

    protected void setTurning(int value) {
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
        this.clearController();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        this.stopControlling();
        this.clearController();
        super.onChunkUnload();
    }
}
