package com.ma4z.andymod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.GeckoLib;

@Mod(AndyMod.MODID)
public class AndyMod {
    public static final String MODID = "andymod";

    public AndyMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        GeckoLib.initialize();
        ModEntities.register(modEventBus);
        modEventBus.addListener(this::registerAttributes);
        modEventBus.addListener(AndyMod::registerRenderers);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ANDY.get(), AndyEntity.createAttributes().build());
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ANDY.get(), AndyRenderer::new);
    }
}