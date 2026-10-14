package anightdazingzoroark.riftedvessels.client.gui;

import anightdazingzoroark.riftedvessels.RiftedVessels;
import anightdazingzoroark.riftedvessels.server.RiftedVesselsUtil;
import anightdazingzoroark.riftedvessels.server.ServerProxy;
import anightdazingzoroark.riftedvessels.server.blocks.RiftedVesselsBlocks;
import anightdazingzoroark.riftedvessels.server.message.AssembleShipMessage;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import valkyrienwarfare.api.IPhysicsEntity;
import valkyrienwarfare.api.IShipData;
import valkyrienwarfare.api.RiftedValkyriumAPI;
import valkyrienwarfare.api.TransformType;

import java.util.Optional;

public class ShipHelmGui extends GuiScreen {
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(
            RiftedVessels.MOD_ID,
            "textures/gui/ship_helm.png"
    );
    private static final int ASSEMBLE_BUTTON_ID = 0;
    private static final int ALIGN_BUTTON_ID = 1;
    private static final int CRUISE_CONTROL_BUTTON_ID = 2;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GUI_WIDTH = 152;
    private static final int GUI_HEIGHT = 116;
    private static final int RELOCATION_TIMEOUT_TICKS = 100;

    private final World world;
    private BlockPos shipHelmPos;
    private GuiButton assembleButton;
    private GuiButton alignButton;
    private boolean assemblyRequested;
    private boolean invalidAssemblyBlock;
    private int missingHelmTicks;

    public ShipHelmGui(@NotNull World world, @NotNull BlockPos shipHelmPos, boolean assemblyRequested) {
        this.world = world;
        this.shipHelmPos = shipHelmPos;
        this.assemblyRequested = assemblyRequested;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        int buttonX = (this.width - BUTTON_WIDTH) / 2;
        int buttonY = (this.height - GUI_HEIGHT) / 2 + 38;

        this.assembleButton = new GuiButton(
                ASSEMBLE_BUTTON_ID, buttonX, buttonY,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                I18n.format("gui.riftedvessels.ship_helm.assemble")
        );
        this.buttonList.add(this.assembleButton);

        this.alignButton = new GuiButton(
                ALIGN_BUTTON_ID, buttonX, buttonY + 24,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                I18n.format("gui.riftedvessels.ship_helm.set_align")
        );
        this.alignButton.enabled = false;
        this.buttonList.add(this.alignButton);

        GuiButton cruiseControlButton = new GuiButton(
                CRUISE_CONTROL_BUTTON_ID, buttonX, buttonY + 48,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                I18n.format("gui.riftedvessels.ship_helm.cruise_control")
        );
        cruiseControlButton.enabled = false;
        this.buttonList.add(cruiseControlButton);

        this.updateAssembleButton();
    }

    @Override
    public void drawDefaultBackground() {
        super.drawDefaultBackground();
        GlStateManager.color(1f, 1f, 1f, 1f);
        this.mc.getTextureManager().bindTexture(BACKGROUND_TEXTURE);
        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;
        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.updateAssembleButton();
        this.drawDefaultBackground();
        int guiTop = (this.height - GUI_HEIGHT) / 2;
        this.drawCenteredString(
                this.fontRenderer,
                I18n.format("gui.riftedvessels.ship_helm"),
                this.width / 2,
                guiTop + 12,
                0xFFFFFF
        );
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (this.invalidAssemblyBlock && this.assembleButton.isMouseOver()) {
            this.drawHoveringText(
                    I18n.format("gui.riftedvessels.ship_helm.invalid_assembly_block"),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    protected void actionPerformed(@NotNull GuiButton button) {
        if (!button.enabled) return;

        switch (button.id) {
            case ASSEMBLE_BUTTON_ID -> {
                boolean assemble = RiftedValkyriumAPI.getShipManagingBlock(this.world, this.shipHelmPos).isEmpty();
                this.assemblyRequested = assemble;
                ServerProxy.MESSAGE_WRAPPER.sendToServer(new AssembleShipMessage(assemble, this.shipHelmPos));
                this.updateAssembleButton();
            }
            case ALIGN_BUTTON_ID -> {

            }
        }
    }

    private void updateAssembleButton() {
        if (this.assembleButton == null) return;

        this.invalidAssemblyBlock = false;
        boolean assembled = RiftedValkyriumAPI.getShipManagingBlock(this.world, this.shipHelmPos).isPresent();
        //disable when it is in the process of assembling or disassembling
        if (this.assemblyRequested != assembled) {
            this.assembleButton.enabled = false;
            String action = this.assemblyRequested ? "assembling" : "disassembling";
            this.assembleButton.displayString = I18n.format("gui.riftedvessels.ship_helm." + action);
        }
        //block if unable to assemble due to invalid block below
        else if (!assembled && RiftedVesselsUtil.isBlockPosBlacklisted(this.world, this.shipHelmPos.down())) {
            this.assembleButton.enabled = false;
            this.assembleButton.displayString = I18n.format("gui.riftedvessels.ship_helm.assemble");
            this.invalidAssemblyBlock = true;
        }
        //set can assemble or disassemble
        else {
            this.assembleButton.enabled = true;
            String action = assembled ? "disassemble" : "assemble";
            this.assembleButton.displayString = I18n.format("gui.riftedvessels.ship_helm." + action);
        }
    }

    private void updateAlignButton() {
        if (this.alignButton == null) return;

        boolean assembled = RiftedValkyriumAPI.getShipManagingBlock(this.world, this.shipHelmPos).isPresent();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.world.getBlockState(this.shipHelmPos).getBlock() == RiftedVesselsBlocks.SHIP_HELM) {
            this.missingHelmTicks = 0;
            return;
        }

        if (this.assemblyRequested && this.followRelocatedHelm()) return;
        this.missingHelmTicks++;
        if (this.assemblyRequested && this.missingHelmTicks <= RELOCATION_TIMEOUT_TICKS) return;
        this.mc.displayGuiScreen(null);
    }

    private boolean followRelocatedHelm() {
        Vec3d previousHelmCenter = new Vec3d(
                this.shipHelmPos.getX() + 0.5D,
                this.shipHelmPos.getY() + 0.5D,
                this.shipHelmPos.getZ() + 0.5D
        );
        for (IShipData shipData : RiftedValkyriumAPI.getAllShips(this.world)) {
            Optional<IPhysicsEntity> ship = RiftedValkyriumAPI.getPhysicsEntity(this.world, shipData);
            if (ship.isEmpty()) continue;

            Vec3d expectedPosition = ship.get().transformVector(previousHelmCenter, TransformType.GLOBAL_TO_SUBSPACE);
            BlockPos expectedHelmPos = new BlockPos(expectedPosition);
            for (BlockPos candidate : BlockPos.getAllInBox(
                    expectedHelmPos.add(-1, -1, -1),
                    expectedHelmPos.add(1, 1, 1)
            )) {
                if (this.world.getBlockState(candidate).getBlock() != RiftedVesselsBlocks.SHIP_HELM) continue;
                this.shipHelmPos = candidate.toImmutable();
                this.missingHelmTicks = 0;
                this.updateAssembleButton();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
