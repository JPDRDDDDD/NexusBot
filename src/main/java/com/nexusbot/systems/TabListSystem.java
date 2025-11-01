package com.nexusbot.systems;

import com.nexusbot.NexusBotMod;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class TabListSystem {

    public void addNexusBotToPlayerTab(ServerPlayerEntity player) {
        try {
            // Método SEGURO - apenas notificar o jogador
            player.sendMessage(new StringTextComponent("§6§lNexusBot §8» §aSistema de moderação online!"), player.getUUID());
            player.sendMessage(new StringTextComponent("§7NexusBot está monitorando o servidor 24/7"), player.getUUID());

            NexusBotMod.LOGGER.info("✅ NexusBot notificado para {}", player.getName().getString());

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao notificar NexusBot: {}", e.toString());
        }
    }

    public void updateBotForAllPlayers() {
        try {
            net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
                    addNexusBotToPlayerTab(player);
                }
                NexusBotMod.LOGGER.info("✅ NexusBot notificado para todos os jogadores");
            }
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao notificar NexusBot para todos: {}", e.toString());
        }
    }
}