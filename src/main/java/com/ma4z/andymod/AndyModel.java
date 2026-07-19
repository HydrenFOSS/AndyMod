package com.ma4z.andymod;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AndyModel extends GeoModel<AndyEntity> {
    
    private static final ResourceLocation DEFAULT_MODEL = new ResourceLocation(AndyMod.MODID, "geo/andyskin.geo.json");
    private static final ResourceLocation EATING_MODEL = new ResourceLocation(AndyMod.MODID, "geo/eating.geo.json");
    
    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(AndyMod.MODID, "textures/entity/andyskin.png");
    private static final ResourceLocation EATING_TEXTURE = new ResourceLocation(AndyMod.MODID, "textures/entity/andy-eating.png");
    private static final ResourceLocation SCARY_TEXTURE = new ResourceLocation(AndyMod.MODID, "textures/entity/andy-scary.png");
    
    private static final ResourceLocation ANIMATION_RESOURCE = new ResourceLocation(AndyMod.MODID, "animations/andy-animation.json");

    @Override
    public ResourceLocation getModelResource(AndyEntity animatable) {
        if (animatable.isEating()) {
            return EATING_MODEL;
        }
        return DEFAULT_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AndyEntity animatable) {
        if (animatable.isWringing() || animatable.isChasing()) {
            return SCARY_TEXTURE;
        }
        if (animatable.isEating()) {
            return EATING_TEXTURE;
        }
        return DEFAULT_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AndyEntity animatable) {
        return ANIMATION_RESOURCE;
    }
}