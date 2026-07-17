package com.ma4z.andymod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AndyMod.MODID);

    public static final RegistryObject<EntityType<AndyEntity>> ANDY = 
            ENTITIES.register("andy", () -> EntityType.Builder.of(AndyEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F) // Standard player hitbox height/width
                    .build("andy"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}