package anightdazingzoroark.riftedvessels;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import valkyrienwarfare.api.ShipBlockBlacklist;

import java.util.HashSet;
import java.util.Set;

public class RiftedVesselsUtil {
    public static ShipBlockBlacklist getBlockBlacklist() {
        Set<Block> blacklist = new HashSet<>();
        for (String blockId : RiftedVesselsConfig.shipHelmBlacklist) {
            Block block = Block.getBlockFromName(blockId);
            if (block != null) blacklist.add(block);
        }
        return (world, position, state) -> blacklist.contains(state.getBlock());
    }

    public static boolean isBlockPosBlacklisted(@NotNull World world, @NotNull BlockPos pos) {
        IBlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();
        if (block.isAir(blockState, world, pos)) return true; //air is automatically blacklisted
        for (String blockIdToTest : RiftedVesselsConfig.shipHelmBlacklist) {
            Block blockToTest = Block.getBlockFromName(blockIdToTest);
            if (block == blockToTest) return true;
        }
        return false;
    }
}
