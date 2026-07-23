package com.ma4z.andymod.event;

import com.ma4z.andymod.AndyEntity;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = "andymod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AndyChatCommandListener {

    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        if (event.getPlayer() == null) return;
        
        List<AndyEntity> andies = event.getPlayer().level().getEntitiesOfClass(AndyEntity.class, event.getPlayer().getBoundingBox().inflate(30.0D));
        for (AndyEntity andy : andies) {
            andy.handlePlayerCommand(event.getPlayer(), event.getRawText());
        }
    }
}