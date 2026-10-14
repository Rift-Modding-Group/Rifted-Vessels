package anightdazingzoroark.riftedvessels.client.renderer.block;

import anightdazingzoroark.riftedvessels.RiftedVessels;
import anightdazingzoroark.riftedvessels.server.tileentities.ShipHelmTileEntity;
import anightdazingzoroark.riftlib.block.AnimatedBlockStateHolder;
import anightdazingzoroark.riftlib.core.controller.AnimationController;
import anightdazingzoroark.riftlib.core.controller.AnimationControllerState;
import anightdazingzoroark.riftlib.core.manager.AnimationDataBlock;
import anightdazingzoroark.riftlib.model.AnimatedGeoModel;
import anightdazingzoroark.riftlib.renderers.geo.GeoBlockRenderer;
import net.minecraft.tileentity.TileEntity;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class ShipHelmBlockRenderer extends GeoBlockRenderer<AnimatedBlockStateHolder> {
    public ShipHelmBlockRenderer() {
        super(
                new AnimatedGeoModel<AnimatedBlockStateHolder>() {
                    @Override
                    @NotNull
                    public String getModId() {
                        return RiftedVessels.MOD_ID;
                    }

                    @Override
                    @NotNull
                    public String getModelIdentifier(AnimatedBlockStateHolder animHolder) {
                        return "geometry.ship_helm";
                    }

                    @Override
                    @NotNull
                    public String getTextureLocation(AnimatedBlockStateHolder animHolder) {
                        return "blocks/ship_helm.png";
                    }
                },
                (world, blockPos, blockStateHolder) -> {
                    return new AnimatedBlockStateHolder(world, blockPos, blockStateHolder){
                        @Override
                        public void initializeAnimationData(@NonNull AnimationDataBlock animData) {
                            TileEntity tileEntity = animData.getHolder().getTileEntity();
                            if (!(tileEntity instanceof ShipHelmTileEntity shipHelmTile)) return;

                            //set turn variable
                            animData.addOnUpdateAnimationValue("Turn", () -> (double) shipHelmTile.getTurning());

                            //set anim controller
                            animData.addAnimationController(
                                    new AnimationController<AnimatedBlockStateHolder, AnimationDataBlock>(this, "turn", "default",
                                            new AnimationControllerState<AnimationDataBlock>("default", 0.5D)
                                                    .addStateTransition("left", data -> data.getVariable("Turn") == 1D)
                                                    .addStateTransition("right", data -> data.getVariable("Turn") == -1D),
                                            new AnimationControllerState<AnimationDataBlock>("left", 0.5D)
                                                    .addAnimation("animation.ship_helm.left")
                                                    .addStateTransition("default", data -> data.getVariable("Turn") == 0D)
                                                    .addStateTransition("right", data -> data.getVariable("Turn") == -1D),
                                            new AnimationControllerState<AnimationDataBlock>("right", 0.5D)
                                                    .addAnimation("animation.ship_helm.right")
                                                    .addStateTransition("default", data -> data.getVariable("Turn") == 0D)
                                                    .addStateTransition("left", data -> data.getVariable("Turn") == 1D)
                                    )
                            );
                        }
                    };
                }
        );
    }
}
