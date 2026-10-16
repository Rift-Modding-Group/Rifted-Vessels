package anightdazingzoroark.riftedvessels.server.blocks;

import anightdazingzoroark.riftedvessels.RiftedVesselsConfig;
import anightdazingzoroark.riftedvessels.server.tileentities.AnchorTileEntity;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

@SuppressWarnings("deprecation")
public class AnchorBlock extends BlockHorizontal implements ITileEntityProvider {
    private static final AxisAlignedBB[] NORTH_COLLISION_BOXES = {
            modelBox(5D, 0D, 13.5D, 11D, 3D, 16.5D),
            modelBox(0.144D, 0.375D, 13.7D, 6.313D, 4.921D, 16.3D),
            modelBox(9.687D, 0.375D, 13.7D, 15.856D, 4.921D, 16.3D),
            modelBox(6.7D, 3D, 13.7D, 9.3D, 7.6D, 16.3D),
            modelBox(7D, 6.8D, 14D, 9D, 13.8D, 16D),
            modelBox(4D, 13.8D, 14D, 12D, 15.8D, 16D),
            modelBox(5.25D, 13.55D, 13.75D, 10.75D, 16.05D, 16.25D),
            modelBox(7D, 15.8D, 14D, 9D, 17.8D, 16D)
    };
    private static final AxisAlignedBB[] SOUTH_COLLISION_BOXES = rotateBoxes(NORTH_COLLISION_BOXES, 2);
    private static final AxisAlignedBB[] WEST_COLLISION_BOXES = rotateBoxes(NORTH_COLLISION_BOXES, 1);
    private static final AxisAlignedBB[] EAST_COLLISION_BOXES = rotateBoxes(NORTH_COLLISION_BOXES, 3);

    private static final AxisAlignedBB NORTH_BOUNDING_BOX = combineBoxes(NORTH_COLLISION_BOXES);
    private static final AxisAlignedBB SOUTH_BOUNDING_BOX = combineBoxes(SOUTH_COLLISION_BOXES);
    private static final AxisAlignedBB WEST_BOUNDING_BOX = combineBoxes(WEST_COLLISION_BOXES);
    private static final AxisAlignedBB EAST_BOUNDING_BOX = combineBoxes(EAST_COLLISION_BOXES);

    public AnchorBlock() {
        super(Material.ANVIL);
        this.setSoundType(SoundType.ANVIL);
    }

    //---collisions---
    @Override
    @NotNull
    public AxisAlignedBB getBoundingBox(
            @NotNull IBlockState state, @NotNull IBlockAccess source, @NotNull BlockPos pos
    ) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_BOUNDING_BOX;
            case WEST -> WEST_BOUNDING_BOX;
            case EAST -> EAST_BOUNDING_BOX;
            default -> NORTH_BOUNDING_BOX;
        };
    }

    @Override
    public void addCollisionBoxToList(
            @NotNull IBlockState state, @NotNull World worldIn, @NotNull BlockPos pos,
            @NotNull AxisAlignedBB entityBox, @NotNull List<AxisAlignedBB> collidingBoxes,
            @Nullable Entity entityIn, boolean isActualState
    ) {
        for (AxisAlignedBB collisionBox : getCollisionBoxes(state)) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, collisionBox);
        }
    }

    @Override
    public boolean isOpaqueCube(@NotNull IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(@NotNull IBlockState state) {
        return false;
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
    //-----block facing stuff ends here-----

    @Override
    @Nullable
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new AnchorTileEntity();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(I18n.format("tile.riftedvessels.anchor.tooltip", RiftedVesselsConfig.shipBalloonMinimumCount));
    }

    @NotNull
    private static AxisAlignedBB[] getCollisionBoxes(@NotNull IBlockState state) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_COLLISION_BOXES;
            case WEST -> WEST_COLLISION_BOXES;
            case EAST -> EAST_COLLISION_BOXES;
            default -> NORTH_COLLISION_BOXES;
        };
    }

    @NotNull
    private static AxisAlignedBB modelBox(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    ) {
        return new AxisAlignedBB(
                minX / 16D, minY / 16D, minZ / 16D,
                maxX / 16D, maxY / 16D, maxZ / 16D
        );
    }

    @NotNull
    private static AxisAlignedBB[] rotateBoxes(@NotNull AxisAlignedBB[] boxes, int quarterTurns) {
        AxisAlignedBB[] rotatedBoxes = new AxisAlignedBB[boxes.length];
        for (int index = 0; index < boxes.length; index++) {
            rotatedBoxes[index] = rotateBox(boxes[index], quarterTurns);
        }
        return rotatedBoxes;
    }

    @NotNull
    private static AxisAlignedBB rotateBox(@NotNull AxisAlignedBB box, int quarterTurns) {
        return switch (quarterTurns) {
            case 1 -> new AxisAlignedBB(
                    box.minZ, box.minY, 1D - box.maxX,
                    box.maxZ, box.maxY, 1D - box.minX
            );
            case 2 -> new AxisAlignedBB(
                    1D - box.maxX, box.minY, 1D - box.maxZ,
                    1D - box.minX, box.maxY, 1D - box.minZ
            );
            case 3 -> new AxisAlignedBB(
                    1D - box.maxZ, box.minY, box.minX,
                    1D - box.minZ, box.maxY, box.maxX
            );
            default -> box;
        };
    }

    @NotNull
    private static AxisAlignedBB combineBoxes(@NotNull AxisAlignedBB[] boxes) {
        AxisAlignedBB combinedBox = boxes[0];
        for (int index = 1; index < boxes.length; index++) {
            combinedBox = combinedBox.union(boxes[index]);
        }
        return combinedBox;
    }
}
