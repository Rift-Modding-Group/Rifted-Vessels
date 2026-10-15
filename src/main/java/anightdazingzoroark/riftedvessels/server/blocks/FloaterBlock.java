package anightdazingzoroark.riftedvessels.server.blocks;

import anightdazingzoroark.riftedvessels.server.tileentities.FloaterTileEntity;
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
import org.jspecify.annotations.Nullable;

import java.util.List;

public class FloaterBlock extends Block implements ITileEntityProvider {
    public FloaterBlock() {
        super(Material.CLOTH);
        this.setSoundType(SoundType.CLOTH);
    }

    @Override
    @Nullable
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new FloaterTileEntity();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(I18n.format("tile.riftedvessels.floater.tooltip_one"));
        tooltip.add(I18n.format("tile.riftedvessels.floater.tooltip_two"));
    }
}
