package com.nexusbot.systems;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import com.nexusbot.NexusBotMod;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

public class PunishmentManager {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Map<String, Integer> offenseCount = new HashMap<>();

    public PunishmentManager() {
        NexusBotMod.LOGGER.info("⚖️ Sistema de punições INICIADO");
    }

    // Sistema de punição automática COM 4 NÍVEIS
    public void applyPunishment(PlayerEntity player, String reason, int level) {
        String playerName = player.getName().getString();
        String playerUUID = player.getStringUUID();

        // Contar ofensas
        int offenses = offenseCount.getOrDefault(playerUUID, 0) + 1;
        offenseCount.put(playerUUID, offenses);

        NexusBotMod.LOGGER.warn("⚖️ NEXUSGUARD PUNIÇÃO: {} - {} (Nível: {} | Ofensa #{})",
                playerName, reason, level, offenses);

        switch (offenses) {
            case 1:
                // 1ª ofensa: Mute 30 minutos
                NexusBotMod.getInstance().getMonitorCore().getChatSystem().mutePlayer(playerName);
                scheduleUnmute(playerName, 30);
                player.sendMessage(new StringTextComponent("§c§l⚠ §cNexusGuard: §cVocê foi mutado por 30 minutos por: " + reason), player.getUUID());
                player.sendMessage(new StringTextComponent("§7⚠ A próxima ofensa resultará em mute de 2 horas!"), player.getUUID());
                break;

            case 2:
                // 2ª ofensa: Mute 2 horas
                NexusBotMod.getInstance().getMonitorCore().getChatSystem().mutePlayer(playerName);
                scheduleUnmute(playerName, 120);
                player.sendMessage(new StringTextComponent("§c§l⚠ §cNexusGuard: §cVocê foi mutado por 2 horas por: " + reason), player.getUUID());
                player.sendMessage(new StringTextComponent("§7⚠ A próxima ofensa resultará em mute de 6 horas!"), player.getUUID());
                break;

            case 3:
                // 3ª ofensa: Mute 6 horas
                NexusBotMod.getInstance().getMonitorCore().getChatSystem().mutePlayer(playerName);
                scheduleUnmute(playerName, 360);
                player.sendMessage(new StringTextComponent("§c§l⚠ §cNexusGuard: §cVocê foi mutado por 6 horas por: " + reason), player.getUUID());
                player.sendMessage(new StringTextComponent("§c§l🚨 §cNEXUSGUARD: §cPRÓXIMA OFENSA: KICK PERMANENTE!"), player.getUUID());
                break;

            case 4:
                // 4ª ofensa: KICK PERMANENTE (simula ban)
                kickPlayerPermanent(player, "Kick permanente: " + reason + " (4ª ofensa)");
                offenseCount.remove(playerUUID);
                break;

            default:
                // Ofensas adicionais: Kick permanente
                kickPlayerPermanent(player, "Kick permanente por reincidência: " + reason);
                break;
        }

        // Log da punição
        NexusBotMod.getInstance().getMonitorCore().getLoggerManager().logPunishment(
                player, reason, "NEXUSGUARD MUTE #" + offenses + " (" + getPunishmentDuration(offenses) + ")"
        );
    }

    // Sistema de unmute automático
    public void scheduleUnmute(String playerName, int minutes) {
        scheduler.schedule(() -> {
            try {
                NexusBotMod.getInstance().getMonitorCore().getChatSystem().unmutePlayer(playerName);
                NexusBotMod.LOGGER.info("⏰ NEXUSGUARD UNMUTE AUTOMÁTICO: {}", playerName);

                // Notificar jogador se estiver online
                if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
                    ServerPlayerEntity target = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerName);
                    if (target != null) {
                        target.sendMessage(new StringTextComponent("§a§l✅ §aNexusGuard: §aSeu mute foi removido automaticamente!"), target.getUUID());
                        target.sendMessage(new StringTextComponent("§7Agora você pode falar no chat novamente."), target.getUUID());
                    }
                }
            } catch (Exception e) {
                NexusBotMod.LOGGER.error("❌ Erro no unmute automático: {}", e.toString());
            }
        }, minutes, TimeUnit.MINUTES);
    }

    // ========== SISTEMA DE KICK PROFISSIONAL ==========
    public void kickPlayer(PlayerEntity player, String reason) {
        try {
            if (player instanceof ServerPlayerEntity) {
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                String kickMessage = "§c§lNEXUSGUARD - MODERAÇÃO AUTOMÁTICA\n\n" +
                        "§fMotivo: §e" + reason + "\n" +
                        "§fTipo: §cKick Automático\n\n" +
                        "§7Você foi removido automaticamente pelo sistema\n" +
                        "§7de proteção NexusGuard.\n\n" +
                        "§c⚠ Reincidência resultará em punições mais severas";

                serverPlayer.connection.disconnect(new StringTextComponent(kickMessage));
                NexusBotMod.LOGGER.warn("👢 NEXUSGUARD PLAYER KICKADO: {} - {}", player.getName().getString(), reason);
            }
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao kickar player: {}", e.toString());
        }
    }

    // ========== SISTEMA DE KICK PERMANENTE ==========
    public void kickPlayerPermanent(PlayerEntity player, String reason) {
        try {
            if (player instanceof ServerPlayerEntity) {
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

                String kickMessage = "§4§lNEXUSGUARD - KICK PERMANENTE\n\n" +
                        "§fMotivo: §c" + reason + "\n" +
                        "§fTipo: §4Kick Permanente\n\n" +
                        "§7Você foi removido permanentemente pelo sistema\n" +
                        "§7de proteção NexusGuard devido a reincidência.\n\n" +
                        "§c🚫 Entre em contato com a administração para apelar";

                serverPlayer.connection.disconnect(new StringTextComponent(kickMessage));
                NexusBotMod.LOGGER.warn("🔨 NEXUSGUARD PLAYER KICK PERMANENTE: {} - {}", player.getName().getString(), reason);
            }
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao kickar player permanentemente: {}", e.toString());
        }
    }

    // ========== SISTEMA DE MUTE MANUAL (para admins) ==========
    public void mutePlayerManual(String playerName, int minutes, String reason) {
        try {
            NexusBotMod.getInstance().getMonitorCore().getChatSystem().mutePlayer(playerName);
            scheduleUnmute(playerName, minutes);

            NexusBotMod.LOGGER.info("🔇 NEXUSGUARD MUTE MANUAL: {} por {} minutos - {}", playerName, minutes, reason);

            // Notificar jogador
            if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
                ServerPlayerEntity target = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerName);
                if (target != null) {
                    target.sendMessage(new StringTextComponent("§c§l🔇 §cNexusGuard: §cVocê foi mutado por um administrador!"), target.getUUID());
                    target.sendMessage(new StringTextComponent("§7Motivo: §f" + reason), target.getUUID());
                    target.sendMessage(new StringTextComponent("§7Duração: §f" + minutes + " minutos"), target.getUUID());
                    target.sendMessage(new StringTextComponent("§7Você não poderá falar no chat até o tempo expirar."), target.getUUID());
                }
            }
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro no mute manual: {}", e.toString());
        }
    }

    // ========== SISTEMA DE UNMUTE MANUAL (para admins) ==========
    public void unmutePlayerManual(String playerName) {
        try {
            NexusBotMod.getInstance().getMonitorCore().getChatSystem().unmutePlayer(playerName);
            NexusBotMod.LOGGER.info("🔊 NEXUSGUARD UNMUTE MANUAL: {}", playerName);

            // Notificar jogador
            if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
                ServerPlayerEntity target = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerName);
                if (target != null) {
                    target.sendMessage(new StringTextComponent("§a§l✅ §aNexusGuard: §aSeu mute foi removido por um administrador!"), target.getUUID());
                    target.sendMessage(new StringTextComponent("§7Agora você pode falar no chat novamente."), target.getUUID());
                }
            }
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro no unmute manual: {}", e.toString());
        }
    }

    // ========== RESETAR CONTADOR DE OFENSAS (para admins) ==========
    public void resetOffenses(String playerName) {
        try {
            if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
                ServerPlayerEntity target = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerName);
                if (target != null) {
                    offenseCount.remove(target.getStringUUID());
                    NexusBotMod.LOGGER.info("🔄 NEXUSGUARD OFENSAS RESETADAS: {}", playerName);

                    target.sendMessage(new StringTextComponent("§a§l✅ §aNexusGuard: §aSeu histórico de ofensas foi resetado!"), target.getUUID());
                    target.sendMessage(new StringTextComponent("§7Você começa com contador zerado."), target.getUUID());
                }
            }
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao resetar ofensas: {}", e.toString());
        }
    }

    // ========== VERIFICAR STATUS DO PLAYER ==========
    public void checkPlayerStatus(String playerName) {
        try {
            if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
                ServerPlayerEntity target = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerName);
                if (target != null) {
                    String playerUUID = target.getStringUUID();
                    int offenses = offenseCount.getOrDefault(playerUUID, 0);
                    boolean isMuted = NexusBotMod.getInstance().getMonitorCore().getChatSystem().isMuted(playerName);

                    NexusBotMod.LOGGER.info("📊 NEXUSGUARD STATUS PLAYER: {}", playerName);
                    NexusBotMod.LOGGER.info("  - Muted: {}", isMuted);
                    NexusBotMod.LOGGER.info("  - Ofensas: {}", offenses);
                    NexusBotMod.LOGGER.info("  - Próxima punição: {}", getNextPunishment(offenses));
                }
            }
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao verificar status: {}", e.toString());
        }
    }

    // ========== MÉTODOS AUXILIARES ==========
    private String getPunishmentDuration(int offenses) {
        switch (offenses) {
            case 1: return "30 minutos";
            case 2: return "2 horas";
            case 3: return "6 horas";
            case 4: return "KICK PERMANENTE";
            default: return "KICK PERMANENTE";
        }
    }

    private String getNextPunishment(int currentOffenses) {
        switch (currentOffenses) {
            case 0: return "Mute 30 minutos";
            case 1: return "Mute 2 horas";
            case 2: return "Mute 6 horas";
            case 3: return "KICK PERMANENTE";
            default: return "KICK PERMANENTE";
        }
    }

    // ========== LIMPEZA DO SCHEDULER ==========
    public void cleanup() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}