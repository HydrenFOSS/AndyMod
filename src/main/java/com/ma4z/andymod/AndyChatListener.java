package com.ma4z.andymod.event;

import com.ma4z.andymod.AndyEntity;
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
                andy.handlePlayerCommand(player, rawMessage);

                double rand = andy.getRandom().nextDouble();
                String appendDuh = (rand < 0.0005) ? " start your text response explicitly with the word 'duh'." : " do NOT use the word 'duh'.";

                String prompt = "You are Andy, a friendly, helpful but laid-back Gen-Z character in Minecraft who talks like they are texting on Discord. "
                        + "Your exact current positions are coordinates: X=" + andy.getBlockX() + ", Y=" + andy.getBlockY() + ", Z=" + andy.getBlockZ() + " "
                        + "A player named " + playerName + " just talked directly to you or gave you a command. "
                        + "Instructions: Always be helpful, follow instructions smoothly, and respond to what they said nicely based on context. Keep your tone funny, relaxed, or slang-heavy. "
                        + "Style: Answers must be extremely short (under 15 words), purely lowercase, use shortcuts like 'wbu', 'rn', 'bruh', 'im fine', 'u doin'. Do not use robotic paragraphs or capitalization. "
                        + "Recent dialogue log for context:\n" + andy.getFormattedHistory() + "\n"
                        + "Random condition fulfilled:" + appendDuh;

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