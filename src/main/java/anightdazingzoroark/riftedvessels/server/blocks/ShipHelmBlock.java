package anightdazingzoroark.riftedvessels.server.blocks;

import anightdazingzoroark.riftedvessels.server.ServerProxy;
import anightdazingzoroark.riftedvessels.server.message.OpenShipHelmGuiMessage;
import anightdazingzoroark.riftedvessels.server.tileentities.ShipHelmTileEntity;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import valkyrienwarfare.api.RiftedValkyriumAPI;

public class ShipHelmBlock extends BlockHorizontal implements ITileEntityProvider {
    private static final AxisAlignedBB HELM_AABB = new AxisAlignedBB(0.125D, 0, 0.125D, 0.875D, 1D, 0.875D);

    public ShipHelmBlock() {
        super(Material.WOOD);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return HELM_AABB;
    }

    @Override
    public IBlockState getStateForPlacement(
            World worldIn, BlockPos pos, EnumFacing facing,
            float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer
    ) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public boolean onBlockActivated(
            World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ
    ) {
        if (worldIn.isRemote || !(playerIn instanceof EntityPlayerMP serverPlayer)) return true;

        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (!(tileEntity instanceof ShipHelmTileEntity shipHelm)) return false;

        //open ship ui
        if (playerIn.isSneaking()) {
            ServerProxy.MESSAGE_WRAPPER.sendTo(new OpenShipHelmGuiMessage(pos, shipHelm.isAssembled()), serverPlayer);
        }
        //attempt to start controlling ship
        else {
            if (shipHelm.isControlledBy(serverPlayer)) {
                shipHelm.stopControlling();
            }
            else if (!shipHelm.isAssembled() || RiftedValkyriumAPI.getPhysoManagingBlock(worldIn, pos).isEmpty()) {
                playerIn.sendStatusMessage(new TextComponentTranslation("warning.riftedvessels.not_assembled"), false);
            }
            else if (shipHelm.isBeingControlled()) {
                playerIn.sendStatusMessage(new TextComponentTranslation("warning.riftedvessels.helm_occupied"), false);
            }
            else {
                shipHelm.startControlling(serverPlayer);
            }
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof ShipHelmTileEntity shipHelm) shipHelm.stopControlling();
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.byIndex(meta);
        if (facing.getAxis() == EnumFacing.Axis.Y) {
            facing = EnumFacing.NORTH;
        }
        return this.getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
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
        return new BlockStateContainer(this, FACING);
    }

    @Override
    @Nullable
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new ShipHelmTileEntity();
    }
}
