package com.ma4z.andymod.sound;

import com.ma4z.andymod.AndyMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class AndySounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AndyMod.MODID);

    public static final RegistryObject<SoundEvent> ANDY_CAVE_SOUND =
            SOUND_EVENTS.register("andy_cave_sound",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AndyMod.MODID, "andy_cave_sound")));

    public static final RegistryObject<SoundEvent> ANDY_NECK_WRING_BREAK =
            SOUND_EVENTS.register("andy_neckwring_break",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AndyMod.MODID, "andy_neckwring_break")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}