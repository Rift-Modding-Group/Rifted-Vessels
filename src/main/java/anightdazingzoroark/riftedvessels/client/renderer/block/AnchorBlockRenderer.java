package anightdazingzoroark.riftedvessels.client.renderer.block;

import anightdazingzoroark.riftedvessels.RiftedVessels;
import anightdazingzoroark.riftlib.block.AnimatedBlockStateHolder;
import anightdazingzoroark.riftlib.core.manager.AnimationDataBlock;
import anightdazingzoroark.riftlib.model.AnimatedGeoModel;
import anightdazingzoroark.riftlib.renderers.geo.GeoBlockRenderer;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class AnchorBlockRenderer extends GeoBlockRenderer<AnimatedBlockStateHolder> {
    public AnchorBlockRenderer() {
        super(
                new AnimatedGeoModel<AnimatedBlockStateHolder>() {
                    @Override
                    @NotNull
                    public String getModId() {
                        return RiftedVessels.MOD_ID;
                    }

                    @Override
                    @NotNull
                    public String getModelIdentifier(AnimatedBlockStateHolder animatedBlockStateHolder) {
                        return "geometry.anchor";
                    }

                    @Override
                    @NotNull
                    public String getTextureLocation(AnimatedBlockStateHolder animatedBlockStateHolder) {
                        return "blocks/anchor.png";
                    }
                },
                (world, blockPos, blockstate) -> {
                    return new AnimatedBlockStateHolder(world, blockPos, blockstate) {
                        @Override
                        public void initializeAnimationData(@NonNull AnimationDataBlock animData) {}
                    };
                }
        );
    }
}
