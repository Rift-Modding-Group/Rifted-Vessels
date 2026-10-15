package anightdazingzoroark.riftedvessels.server.blocks;

import anightdazingzoroark.riftedvessels.server.tileentities.ShipEngineTileEntity;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("deprecation")
public class ShipEngineBlock extends BlockHorizontal implements ITileEntityProvider {
    public static PropertyBool BURNING = PropertyBool.create("burning");

    public ShipEngineBlock() {
        super(Material.ROCK);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(BURNING, false));
    }

    @Override
    public boolean onBlockActivated(
            World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ
    ) {
        if (worldIn.isRemote || !(playerIn instanceof EntityPlayerMP serverPlayer)) return true;

        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (!(tileEntity instanceof ShipEngineTileEntity shipEngine)) return false;

        ItemStack heldItem = serverPlayer.getHeldItem(hand);

        //insert fuel
        if (!heldItem.isEmpty() && ShipEngineTileEntity.isFuel(heldItem)) {
            ItemStack remainingFuel = shipEngine.insertFuel(heldItem);
            if (remainingFuel.getCount() != heldItem.getCount()) {
                serverPlayer.setHeldItem(hand, remainingFuel);
                serverPlayer.inventoryContainer.detectAndSendChanges();
                worldIn.playSound(
                        null, pos.getX(), pos.getY(), pos.getZ(),
                        SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.BLOCKS, 1F, 1F
                );
            }
        }
        //remove fuel
        else if (serverPlayer.isSneaking() && heldItem.isEmpty()) {
            ItemStack removedFuel = shipEngine.removeFuel();
            if (!removedFuel.isEmpty()) {
                if (!serverPlayer.addItemStackToInventory(removedFuel)) serverPlayer.dropItem(removedFuel, false);
                serverPlayer.inventoryContainer.detectAndSendChanges();
            }
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof ShipEngineTileEntity shipEngine) shipEngine.dropFuel();
        super.breakBlock(worldIn, pos, state);
    }

    //-----block facing stuff begins here-----
    @Override
    public IBlockState getStateForPlacement(
            World worldIn, BlockPos pos, EnumFacing facing,
            float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer
    ) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.byIndex(meta & 7);
        if (facing.getAxis() == EnumFacing.Axis.Y) {
            facing = EnumFacing.NORTH;
        }
        return this.getDefaultState()
                .withProperty(FACING, facing)
                .withProperty(BURNING, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getIndex();
        if (state.getValue(BURNING)) meta |= 8;
        return meta;
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, BURNING);
    }
    //-----block facing stuff ends here-----

    @Override
    @Nullable
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new ShipEngineTileEntity();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(I18n.format("tile.riftedvessels.ship_engine.tooltip"));
    }
}
