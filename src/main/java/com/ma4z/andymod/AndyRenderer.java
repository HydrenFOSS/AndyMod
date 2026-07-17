package com.ma4z.andymod;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AndyRenderer extends GeoEntityRenderer<AndyEntity> {
    public AndyRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AndyModel());
    }
}