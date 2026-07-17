package com.ma4z.andymod;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AndyModel extends GeoModel<AndyEntity> {
    @Override
    public ResourceLocation getModelResource(AndyEntity animatable) {
        return new ResourceLocation(AndyMod.MODID, "geo/andySkin.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AndyEntity animatable) {
        return new ResourceLocation(AndyMod.MODID, "textures/entity/andySkin.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AndyEntity animatable) {
        return new ResourceLocation(AndyMod.MODID, "animations/wallking-animation.json");
    }
}