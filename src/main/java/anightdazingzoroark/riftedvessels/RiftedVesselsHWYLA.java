package anightdazingzoroark.riftedvessels;

import anightdazingzoroark.riftedvessels.server.tileentities.ShipEngineTileEntity;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.IWailaRegistrar;
import mcp.mobius.waila.api.SpecialChars;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@WailaPlugin(RiftedVessels.MOD_ID)
public class RiftedVesselsHWYLA implements IWailaPlugin {
    @Override
    public void register(@NotNull IWailaRegistrar registrar) {
        registrar.registerBodyProvider(
                new IWailaDataProvider() {
                    @Override
                    @SideOnly(Side.CLIENT)
                    @NotNull
                    public List<String> getWailaBody(
                            @NotNull ItemStack itemStack, @NotNull List<String> tooltip,
                            @NotNull IWailaDataAccessor accessor, @NotNull IWailaConfigHandler config
                    ) {
                        NBTTagCompound tag = accessor.getNBTData();
                        if (!tag.hasKey("RiftedVesselsFuel", 10)) return tooltip;

                        NBTTagCompound fuelTag = tag.getCompoundTag("RiftedVesselsFuel");
                        ItemStack fuel = new ItemStack(fuelTag);
                        if (fuel.isEmpty()) {
                            tooltip.add(SpecialChars.getRenderString("waila.stack", "2"));
                        }
                        else {
                            tooltip.add(SpecialChars.getRenderString(
                                    "waila.stack", "1", fuel.getItem().getRegistryName().toString(),
                                    Integer.toString(fuel.getCount()), Integer.toString(fuel.getItemDamage()),
                                    fuelTag.getCompoundTag("tag").toString()
                            ));
                        }
                        return tooltip;
                    }
                },
                ShipEngineTileEntity.class
        );

        registrar.registerNBTProvider(
                new IWailaDataProvider() {
                    @Override
                    @NotNull
                    public NBTTagCompound getNBTData(
                            @NotNull EntityPlayerMP player, @NotNull TileEntity tileEntity, @NotNull NBTTagCompound tag,
                            @NotNull World world, @NotNull BlockPos pos
                    ) {
                        if (tileEntity instanceof ShipEngineTileEntity engine) {
                            ItemStack fuel = engine.getInventory("FuelInventory").getStackInSlot(0);
                            tag.setTag("RiftedVesselsFuel", fuel.writeToNBT(new NBTTagCompound()));
                        }
                        return tag;
                    }
                },
                ShipEngineTileEntity.class
        );
    }
}
