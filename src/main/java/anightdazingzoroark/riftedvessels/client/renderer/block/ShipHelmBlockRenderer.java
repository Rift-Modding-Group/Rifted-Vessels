package anightdazingzoroark.riftedvessels.client.renderer.block;

import anightdazingzoroark.riftedvessels.RiftedVessels;
import anightdazingzoroark.riftlib.block.AnimatedBlockStateHolder;
import anightdazingzoroark.riftlib.core.manager.AnimationDataBlock;
import anightdazingzoroark.riftlib.model.AnimatedGeoModel;
import anightdazingzoroark.riftlib.renderers.geo.GeoBlockRenderer;
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

                        }
                    };
                }
        );
    }
}
