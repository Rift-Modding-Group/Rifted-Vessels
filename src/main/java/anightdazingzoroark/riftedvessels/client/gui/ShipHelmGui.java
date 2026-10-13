package anightdazingzoroark.riftedvessels.client.gui;

import anightdazingzoroark.riftedvessels.RiftedVessels;
import anightdazingzoroark.riftedvessels.server.tileentities.ShipHelmTileEntity;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

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

    private final ShipHelmTileEntity shipHelm;

    public ShipHelmGui(ShipHelmTileEntity shipHelm) {
        this.shipHelm = shipHelm;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        int buttonX = (this.width - BUTTON_WIDTH) / 2;
        int buttonY = (this.height - GUI_HEIGHT) / 2 + 38;

        this.buttonList.add(new GuiButton(
                ASSEMBLE_BUTTON_ID, buttonX, buttonY,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                I18n.format("gui.riftedvessels.ship_helm.assemble")
        ));

        GuiButton alignButton = new GuiButton(
                ALIGN_BUTTON_ID, buttonX, buttonY + 24,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                I18n.format("gui.riftedvessels.ship_helm.align")
        );
        alignButton.enabled = false;
        this.buttonList.add(alignButton);

        GuiButton cruiseControlButton = new GuiButton(
                CRUISE_CONTROL_BUTTON_ID, buttonX, buttonY + 48,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                I18n.format("gui.riftedvessels.ship_helm.cruise_control")
        );
        cruiseControlButton.enabled = false;
        this.buttonList.add(cruiseControlButton);
    }

    @Override
    public void drawDefaultBackground() {
        super.drawDefaultBackground();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(BACKGROUND_TEXTURE);
        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;
        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
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
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
