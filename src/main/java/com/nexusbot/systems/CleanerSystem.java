package com.nexusbot.systems;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.ChatType;
import com.nexusbot.NexusBotMod;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;

public class CleanerSystem {
    private ScheduledExecutorService scheduler;
    private boolean isActive = true;
    private int cleanupInterval = 45; // ✅ 45 MINUTOS
    private int lastCleanedItems = 0;
    private int lastCleanedMobs = 0;
    private int warningCountdown = 0;

    // Entidades PROTEGIDAS
    private final Set<String> protectedMobs = new HashSet<String>() {{
        add("minecraft:ender_dragon");
        add("minecraft:wither");
        add("minecraft:elder_guardian");
        add("boss");
        add("boss_");
        add(":boss");
    }};

    // Mobs que SEMPRE são removidos
    private final Set<String> alwaysCleanMobs = new HashSet<String>() {{
        add("minecraft:zombie");
        add("minecraft:skeleton");
        add("minecraft:creeper");
        add("minecraft:spider");
        add("minecraft:cave_spider");
        add("minecraft:enderman");
        add("minecraft:witch");
        add("minecraft:blaze");
        add("minecraft:ghast");
        add("minecraft:magma_cube");
        add("minecraft:slime");
        add("minecraft:silverfish");
        add("minecraft:endermite");
        add("minecraft:phantom");
        add("minecraft:drowned");
        add("minecraft:husk");
        add("minecraft:stray");
        add("minecraft:bat");
        add("minecraft:squid");
    }};

    public CleanerSystem() {
        NexusBotMod.LOGGER.info("🗑️ Sistema de limpeza automática INICIADO - Intervalo: 45 minutos");
        startAutoCleanup();
        startWarningSystem();
    }

    // ========== SISTEMA DE LIMPEZA AUTOMÁTICA - 45 MINUTOS ==========
    private void startAutoCleanup() {
        scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(() -> {
            if (isActive) {
                performCleanup();
            }
        }, cleanupInterval, cleanupInterval, TimeUnit.MINUTES);

        NexusBotMod.LOGGER.info("⏰ Limpeza automática a cada {} minutos", cleanupInterval);
    }

    // ========== SISTEMA DE AVISOS AUTOMÁTICOS ==========
    private void startWarningSystem() {
        ScheduledExecutorService warningScheduler = Executors.newScheduledThreadPool(1);

        // Verifica a cada minuto
        warningScheduler.scheduleAtFixedRate(() -> {
            if (isActive) {
                sendWarnings();
            }
        }, 1, 1, TimeUnit.MINUTES);

        NexusBotMod.LOGGER.info("🔔 Sistema de avisos automáticos INICIADO");
    }

    // ========== ENVIAR AVISOS DE LIMPEZA ==========
    private void sendWarnings() {
        try {
            if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() == null) return;

            // Calcular tempo restante para próxima limpeza
            long nextCleanup = getNextCleanupTime();
            long currentTime = System.currentTimeMillis();
            long timeLeft = nextCleanup - currentTime;
            long minutesLeft = TimeUnit.MILLISECONDS.toMinutes(timeLeft);

            // Enviar avisos em intervalos específicos
            if (minutesLeft == 5) {
                broadcastWarning("⚠️ §6§lAVISO: §eLimpeza automática em §c5 minutos§e! §7(Remove itens dropados para reduzir lag)");
            } else if (minutesLeft == 3) {
                broadcastWarning("⚠️ §6§lAVISO: §eLimpeza automática em §c3 minutos§e! §7(Salve seus itens importantes)");
            } else if (minutesLeft == 1) {
                broadcastWarning("⚠️ §6§lAVISO: §eLimpeza automática em §c1 minuto§e! §7(Itens no chão serão removidos)");
            } else if (minutesLeft == 0 && timeLeft > 0) {
                broadcastWarning("🔔 §6§lAVISO FINAL: §eLimpeza automática em §c30 segundos§e! §4⚠ ITENS NO CHÃO SERÃO REMOVIDOS!");
            }

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro no sistema de avisos: {}", e.toString());
        }
    }

    private long getNextCleanupTime() {
        // Calcular próxima limpeza baseado no intervalo de 45 minutos
        long currentTime = System.currentTimeMillis();
        long cleanupIntervalMs = TimeUnit.MINUTES.toMillis(cleanupInterval);

        // Encontrar o próximo múltiplo de 45 minutos
        long nextCleanup = ((currentTime / cleanupIntervalMs) + 1) * cleanupIntervalMs;
        return nextCleanup;
    }

    private void broadcastWarning(String message) {
        if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
            net.minecraft.util.text.ITextComponent text = new StringTextComponent(message);
            net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getPlayerList()
                    .broadcastMessage(text, ChatType.SYSTEM, Util.NIL_UUID);

            NexusBotMod.LOGGER.info("🔔 AVISO: {}", message.replaceAll("§.", ""));
        }
    }

    // ========== LIMPEZA PROFISSIONAL - 100% DOS ITENS ==========
    public void performCleanup() {
        try {
            if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() == null) return;

            int cleanedItems = 0;
            int cleanedMobs = 0;

            // ✅ AVISO IMEDIATAMENTE ANTES DA LIMPEZA
            broadcastToAllPlayers("🔔 §6§l🗑️ LIMPEZA AUTOMÁTICA INICIADA! §7(Removendo itens dropados para reduzir lag)");

            for (net.minecraft.world.server.ServerWorld world :
                    net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
                for (Entity entity : world.getAllEntities()) {
                    if (shouldCleanEntity(entity)) {
                        if (entity instanceof ItemEntity) {
                            entity.remove();
                            cleanedItems++;
                        } else {
                            entity.remove();
                            cleanedMobs++;
                        }
                    }
                }
            }

            lastCleanedItems = cleanedItems;
            lastCleanedMobs = cleanedMobs;

            // ✅ MENSAGEM DE RESULTADO
            String announcement = String.format(
                    "§6§l🗑️ Limpeza Automática CONCLUÍDA §8» §a%s itens §7e §c%s monstros §7removidos\n§7§o✅ Lag reduzido - Servidor otimizado",
                    cleanedItems, cleanedMobs
            );

            broadcastToAllPlayers(announcement);
            NexusBotMod.LOGGER.info("🧹 LIMPEZA: {} itens e {} monstros removidos - LAG REDUZIDO", cleanedItems, cleanedMobs);

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro na limpeza automática: {}", e.toString());
        }
    }

    // ========== VERIFICAÇÃO DE ENTIDADES - 100% DOS ITENS ==========
    private boolean shouldCleanEntity(Entity entity) {
        if (entity == null) return false;

        // NUNCA remover jogadores
        if (entity instanceof PlayerEntity) return false;

        // ✅ REMOVER 100% DOS ITENS DROPADOS (SEM RESTRIÇÕES) - ANTI-LAG
        if (entity instanceof ItemEntity) {
            return true; // ✅ REMOVE TODOS os itens dropados IMEDIATAMENTE
        }

        // NUNCA remover entidades com nome customizado
        if (entity.hasCustomName()) return false;

        // NUNCA remover entidades recentes (menos de 2 minutos) - APENAS PARA MOBS
        if (entity.tickCount < 2400) {
            return false; // NÃO limpa MOBS com menos de 2 minutos
        }

        // Verificar proteções específicas
        if (isProtectedEntity(entity)) {
            return false;
        }

        // Verificar se é mob comum para limpar
        return isCommonMob(entity);
    }

    private boolean isProtectedEntity(Entity entity) {
        // PROTEGER villagers e iron golems
        if (entity instanceof VillagerEntity || entity instanceof IronGolemEntity) return true;

        // PROTEGER pets e animais
        if (entity instanceof WolfEntity || entity instanceof CatEntity || entity instanceof ParrotEntity ||
                entity instanceof HorseEntity || entity instanceof AbstractHorseEntity ||
                entity instanceof FoxEntity || entity instanceof PandaEntity ||
                entity instanceof BeeEntity || entity instanceof DolphinEntity ||
                entity instanceof TurtleEntity) return true;

        // PROTEGER animais de fazenda
        if (entity instanceof CowEntity || entity instanceof SheepEntity || entity instanceof PigEntity ||
                entity instanceof ChickenEntity || entity instanceof RabbitEntity) return true;

        // PROTEGER mobs especiais e bosses
        if (entity.getType().getRegistryName() != null) {
            String mobId = entity.getType().getRegistryName().toString();

            for (String protectedMob : protectedMobs) {
                if (mobId.contains(protectedMob)) {
                    return true;
                }
            }

            if (mobId.equals("minecraft:ender_dragon") || mobId.equals("minecraft:wither")) {
                return true;
            }
        }

        return false;
    }

    private boolean isCommonMob(Entity entity) {
        if (entity.getType().getRegistryName() == null) return false;

        String mobId = entity.getType().getRegistryName().toString();
        return alwaysCleanMobs.contains(mobId);
    }

    // ========== SISTEMA DE CONTROLE ==========
    public void setActive(boolean active) {
        this.isActive = active;
        String status = active ? "§aATIVADO" : "§cDESATIVADO";
        broadcastToOps("§6§l🗑️ Sistema de limpeza " + status + "§6! §7(Intervalo: 45 minutos)");
        NexusBotMod.LOGGER.info("🔧 Limpeza automática: {} | Intervalo: 45 minutos", active ? "ATIVADA" : "DESATIVADA");
    }

    public void setInterval(int minutes) {
        this.cleanupInterval = Math.max(1, minutes);
        restartScheduler();

        broadcastToOps(String.format("§6§l⏰ Intervalo de limpeza alterado para §e%d minutos§6!", cleanupInterval));
        NexusBotMod.LOGGER.info("⏰ Intervalo de limpeza alterado para {} minutos", cleanupInterval);
    }

    public void forceCleanup() {
        broadcastToOps("§6§l🧹 Limpeza manual iniciada... §7(Removendo itens para reduzir lag)");
        broadcastToAllPlayers("🔔 §6§l🧹 LIMPEZA MANUAL INICIADA §7(Removendo itens dropados)");
        performCleanup();
    }

    public String getStatus() {
        String status = isActive ? "§a✔ ATIVO" : "§c✘ INATIVO";
        long nextCleanup = getNextCleanupTime();
        long currentTime = System.currentTimeMillis();
        long minutesLeft = TimeUnit.MILLISECONDS.toMinutes(nextCleanup - currentTime);
        String nextCleanupStr = isActive ? "próxima em " + minutesLeft + " min" : "sistema pausado";

        return String.format(
                "§6🗑️ §lSistema de Limpeza Automática §8(ANTI-LAG)\n" +
                        "§7Status: %s\n" +
                        "§7Intervalo: §e%d minutos\n" +
                        "§7Próxima limpeza: §f%s\n" +
                        "§7Última limpeza: §a%d itens§7, §c%d monstros\n" +
                        "§7\n" +
                        "§c⚡ Remove: §f100% dos itens dropados §7(ANTI-LAG)\n" +
                        "§7Remove: mobs comuns (após 2 minutos)\n" +
                        "§a🛡️ Protege: §fpets, villagers, bosses, mobs nomeados\n" +
                        "§6🔔 Avisos: §f5min, 3min, 1min, 30s antes da limpeza",
                status, cleanupInterval, nextCleanupStr, lastCleanedItems, lastCleanedMobs
        );
    }

    // ========== SISTEMA DE COMUNICAÇÃO ==========
    private void broadcastToAllPlayers(String message) {
        if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
            net.minecraft.util.text.ITextComponent text = new StringTextComponent(message);
            net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getPlayerList()
                    .broadcastMessage(text, ChatType.SYSTEM, Util.NIL_UUID);
        }
    }

    private void broadcastToOps(String message) {
        if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayerEntity player :
                    net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (player.hasPermissions(2)) {
                    player.sendMessage(new StringTextComponent(message), Util.NIL_UUID);
                }
            }
        }
    }

    // ========== CONTROLE DO SCHEDULER ==========
    private void restartScheduler() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        startAutoCleanup();
    }

    public void cleanup() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        NexusBotMod.LOGGER.info("🔧 Sistema de limpeza encerrado");
    }
}