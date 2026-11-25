package anightdazingzoroark.riftedvessels.server.blocks;

import anightdazingzoroark.riftedvessels.RiftedVesselsConfig;
import anightdazingzoroark.riftedvessels.server.tileentities.SubmarineTankTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SubmarineTankBlock extends Block implements ITileEntityProvider {
    public SubmarineTankBlock() {
        super(Material.IRON);
        this.setSoundType(SoundType.METAL);
        this.setHardness(3f);
        this.setResistance(5f);
        this.setHarvestLevel("pickaxe", 1);
    }

    @Override
    @NotNull
    public TileEntity createNewTileEntity(@NotNull World worldIn, int meta) {
        return new SubmarineTankTileEntity();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(@NotNull ItemStack stack, @Nullable World worldIn, @NotNull List<String> tooltip, @NotNull ITooltipFlag flagIn) {
        tooltip.add(I18n.format("tile.riftedvessels.submarine_tank.tooltip", RiftedVesselsConfig.shipSubmarineTankMinimumCount));
    }
}
