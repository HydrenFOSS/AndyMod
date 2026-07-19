package com.ma4z.andymod.event;

import com.ma4z.andymod.AndyEntity;
import com.ma4z.andymod.AndyPrompt;
import com.ma4z.andymod.ai.AIAgent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = "andymod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AndyChatListener {

    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) return;

        String rawMessage = event.getRawText();
        String playerName = player.getGameProfile().getName();

        AABB searchBox = player.getBoundingBox().inflate(16.0D);
        List<AndyEntity> nearbyAndies = player.level().getEntitiesOfClass(AndyEntity.class, searchBox);

        for (AndyEntity andy : nearbyAndies) {
            if (andy.isAlive()) {
                andy.addLogToHistory(playerName, rawMessage);
                
                boolean commandHandled = andy.handlePlayerCommand(player, rawMessage);
                if (commandHandled) {
                    continue;
                }

                double rand = andy.getRandom().nextDouble();
                String appendDuh = (rand < 0.0005) ? " start your text response explicitly with the word 'duh'." : " do NOT use the word 'duh'.";

                String prompt = AndyPrompt.getPromptForMood(andy.getMood(), playerName, andy.getBlockX(), andy.getBlockY(), andy.getBlockZ(), andy.getFormattedHistory(), appendDuh, null);

                AIAgent.sendPromptAsync(prompt).thenAccept(response -> {
                    andy.level().getServer().execute(() -> {
                        if (andy.isAlive()) {
                            andy.broadcastToNearbyPlayers(response);
                        }
                    });
                });
            }
        }
    }
}