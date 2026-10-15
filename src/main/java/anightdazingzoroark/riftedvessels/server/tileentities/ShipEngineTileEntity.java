package anightdazingzoroark.riftedvessels.server.tileentities;

import anightdazingzoroark.riftedvessels.server.blocks.ShipEngineBlock;
import anightdazingzoroark.riftlib.inventory.RiftLibInventoryHandler;
import anightdazingzoroark.riftlib.nbtStorageUser.propertyValue.IntegerPropertyValue;
import anightdazingzoroark.riftlib.nbtStorageUser.tileEntity.RiftTileEntityContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import valkyrienwarfare.api.IPhysicsEntity;
import valkyrienwarfare.api.RiftedValkyriumAPI;

import java.util.Optional;

public class ShipEngineTileEntity extends RiftTileEntityContainer implements ITickable {
    @Nullable
    private ShipEngineController controller;

    @Override
    public void update() {
        if (this.world.isRemote) return;

        //find a ship
        Optional<IPhysicsEntity> ship = RiftedValkyriumAPI.getPhysoManagingBlock(this.world, this.pos);
        if (ship.isPresent()) this.setControllerShip(ship.get());
        else this.clearController();

        //update burning
        if (this.isBurning()) this.setBurnTime(this.getBurnTime() - 1);

        //start burning when there's fuel
        if (!this.isBurning() && this.controller != null) {
            RiftLibInventoryHandler fuelInventory = this.getInventory("FuelInventory");
            ItemStack fuelStack = fuelInventory.getStackInSlot(0);
            int burnTime = TileEntityFurnace.getItemBurnTime(fuelStack);
            if (burnTime > 0) {
                ItemStack consumedFuel = fuelStack.copy();
                consumedFuel.setCount(1);
                ItemStack containerItem = consumedFuel.getItem().getContainerItem(consumedFuel);
                fuelStack.shrink(1);
                if (fuelStack.isEmpty()) fuelInventory.setStackInSlot(0, containerItem);
                this.setBurnTime(burnTime);
            }
        }

        //update controller
        if (this.controller != null) this.controller.tick();
    }

    @Override
    public void registerValues() {
        this.registerValue(new IntegerPropertyValue("BurnTime", 0));
    }

    @Override
    public void registerInventories() {
        this.registerInventory("FuelInventory", new RiftLibInventoryHandler(1, ShipEngineTileEntity::isFuel));
        this.registerInventorySiding(
                "FuelInventory", SideInvInteraction.INSERT,
                EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST
        );
        this.registerInventorySiding("FuelInventory", SideInvInteraction.EXTRACT, EnumFacing.DOWN);
        this.finalizeInventorySidingInfo();
    }

    @Override
    public void registerFluidTanks() {}

    //---controller stuff---
    private void setControllerShip(@NotNull IPhysicsEntity ship) {
        if (this.controller != null && this.controller.manages(ship)) return;
        this.clearController();
        this.controller = ShipEngineController.attach(ship, this);
    }

    private void clearController() {
        if (this.controller == null) return;
        ShipEngineController previousController = this.controller;
        this.controller = null;
        ShipEngineController.release(previousController, this);
    }

    //---fuel management---
    public static boolean isFuel(@NotNull ItemStack stack) {
        return !stack.isEmpty() && TileEntityFurnace.isItemFuel(stack);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return super.canExtractItem(index, stack, direction) && !isFuel(stack);
    }

    public ItemStack insertFuel(@NotNull ItemStack stack) {
        if (!isFuel(stack)) return stack;
        ItemStack remainder = this.getInventory("FuelInventory").insertItem(0, stack, false);
        if (remainder.getCount() != stack.getCount()) this.updateServerData();
        return remainder;
    }

    public ItemStack removeFuel() {
        RiftLibInventoryHandler fuelInventory = this.getInventory("FuelInventory");
        ItemStack removedFuel = fuelInventory.extractItem(0, fuelInventory.getSlotLimit(0), false);
        if (!removedFuel.isEmpty()) {
            if (this.isBurning()) this.setBurnTime(0);
            else this.updateServerData();
        }
        return removedFuel;
    }

    public void dropFuel() {
        if (this.world == null) return;
        this.getInventory("FuelInventory").dropAllItems(this.world, this.pos);
    }

    //---setters n getters for sync values---
    private void setBurnTime(int value) {
        int burnTime = Math.max(value, 0);
        if (burnTime == this.getBurnTime()) return;
        boolean wasBurning = this.isBurning();
        this.setValue("BurnTime", burnTime);

        //update block state
        IBlockState state = this.world.getBlockState(this.pos);
        if (wasBurning != this.isBurning() && state.getBlock() instanceof ShipEngineBlock && state.getValue(ShipEngineBlock.BURNING) != this.isBurning()) {
            this.world.setBlockState(this.pos, state.withProperty(ShipEngineBlock.BURNING, this.isBurning()), 3);
        }
    }

    public int getBurnTime() {
        return this.getValue("BurnTime");
    }

    public boolean isBurning() {
        return this.getBurnTime() > 0;
    }

    //---misc overrides---
    @Override
    public boolean shouldRefresh(
            @NotNull World world, @NotNull BlockPos pos,
            @NotNull IBlockState oldState, @NotNull IBlockState newState
    ) {
        return oldState.getBlock() != newState.getBlock();
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

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index == 0 && isFuel(stack);
    }
}
