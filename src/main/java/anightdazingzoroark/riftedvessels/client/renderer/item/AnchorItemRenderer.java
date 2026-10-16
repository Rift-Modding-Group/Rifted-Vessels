package anightdazingzoroark.riftedvessels.client.renderer.item;

import anightdazingzoroark.riftedvessels.RiftedVessels;
import anightdazingzoroark.riftlib.core.manager.AnimationDataItemStack;
import anightdazingzoroark.riftlib.item.AnimatedItemStackHolder;
import anightdazingzoroark.riftlib.model.AnimatedGeoModel;
import anightdazingzoroark.riftlib.renderers.geo.GeoItemRenderer;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class AnchorItemRenderer extends GeoItemRenderer<AnimatedItemStackHolder> {
    public AnchorItemRenderer() {
        super(
                new AnimatedGeoModel<AnimatedItemStackHolder>() {
                    @Override
                    @NotNull
                    public String getModId() {
                        return RiftedVessels.MOD_ID;
                    }

                    @Override
                    @NotNull
                    public String getModelIdentifier(AnimatedItemStackHolder animHolder) {
                        return "geometry.anchor";
                    }

                    @Override
                    @NotNull
                    public String getTextureLocation(AnimatedItemStackHolder animHolder) {
                        return "blocks/anchor.png";
                    }
                },
                itemStack -> new AnimatedItemStackHolder(itemStack) {
                    @Override
                    public void initializeAnimationData(@NonNull AnimationDataItemStack animData) {}
                }
        );
    }
}
