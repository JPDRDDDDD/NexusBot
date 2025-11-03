package com.nexusbot.systems;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import com.nexusbot.NexusBotMod;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LoggerManager {
    private final String LOGS_FOLDER = "nexusbot_logs";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private Map<String, PrintWriter> playerWriters = new HashMap<>();

    public LoggerManager() {
        createLogsFolder();
        startAutoSave();
        NexusBotMod.LOGGER.info("📊 Sistema de logs 24/7 INICIADO");
    }

    private void createLogsFolder() {
        File logsFolder = new File(LOGS_FOLDER);
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }
    }

    private PrintWriter getPlayerWriter(PlayerEntity player) {
        String playerId = player.getStringUUID() + "_" + player.getName().getString().toLowerCase();
        String logFile = LOGS_FOLDER + "/" + playerId + ".log";

        try {
            if (!playerWriters.containsKey(playerId)) {
                FileWriter fileWriter = new FileWriter(logFile, true);
                PrintWriter writer = new PrintWriter(new BufferedWriter(fileWriter), true);
                playerWriters.put(playerId, writer);

                writer.println("=== NEXUSBOT LOGS - " + player.getName().getString() + " ===");
                writer.println("Iniciado em: " + dateFormat.format(new Date()));
                writer.println("UUID: " + player.getStringUUID());
                writer.println("=================================");
            }
            return playerWriters.get(playerId);
        } catch (IOException e) {
            NexusBotMod.LOGGER.error("❌ Erro ao criar writer: {}", e.getMessage());
            return null;
        }
    }

    private void logToFile(PlayerEntity player, String category, String message) {
        try {
            PrintWriter writer = getPlayerWriter(player);
            if (writer != null) {
                String timestamp = dateFormat.format(new Date());
                String logEntry = String.format("[%s] [%s] %s", timestamp, category, message);
                writer.println(logEntry);
                writer.flush();
            }
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao escrever log: {}", e.getMessage());
        }
    }

    // ========== SISTEMA DE NOTIFICAÇÃO PARA NEXUSBOT ==========
    private void notifyBotActivity(PlayerEntity player, String action, String details) {
        try {
            ChatSystem chatSystem = NexusBotMod.getInstance().getMonitorCore().getChatSystem();
            chatSystem.updatePlayerActivity(player, action + ": " + details);
            NexusBotMod.LOGGER.info("🔔 Notificando NexusBot: {} - {} - {}",
                    player.getName().getString(), action, details);
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao notificar NexusBot: {}", e.toString());
        }
    }

    private void startAutoSave() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000);
                    flushAllWriters();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    private void flushAllWriters() {
        for (PrintWriter writer : playerWriters.values()) {
            if (writer != null) {
                writer.flush();
            }
        }
    }

    // ========== SISTEMA DE LOGS COMPLETO ==========

    public void logChat(PlayerEntity player, String message) {
        logToFile(player, "CHAT", "\"" + message + "\"");
        if (message.length() > 5) {
            notifyBotActivity(player, "CHAT", "disse: " + (message.length() > 20 ? message.substring(0, 20) + "..." : message));
        }
    }

    public void logJoin(PlayerEntity player) {
        logToFile(player, "SISTEMA", "Jogador entrou no servidor");
        logToFile(player, "SISTEMA", "Posição: " + getPositionString(player));
        logPlayerEffects(player, "ENTRADA");
        notifyBotActivity(player, "ENTRADA", "entrou no servidor");
    }

    public void logLeave(PlayerEntity player) {
        logToFile(player, "SISTEMA", "Jogador saiu do servidor");
        logToFile(player, "SISTEMA", "Posição final: " + getPositionString(player));
        logFullPlayerInventory(player, "SAIDA");
        logPlayerEffects(player, "SAIDA");
        notifyBotActivity(player, "SAIDA", "saiu do servidor");
    }

    public void logCommand(PlayerEntity player, String command) {
        logToFile(player, "COMANDO", "/" + command);
        if (!command.matches("(l|g|s|tell|msg|w)")) {
            notifyBotActivity(player, "COMANDO", "usou: /" + command);
        }
        NexusBotMod.LOGGER.info("⌨️ {} executou: /{}", player.getName().getString(), command);
    }

    // ========== SISTEMA DE MORTE COMPLETA ==========
    public void logDeathWithFullInventory(PlayerEntity player, String cause) {
        logToFile(player, "MORTE", "Morreu: " + cause);
        logToFile(player, "MORTE", "Posição: " + getPositionString(player));
        logFullPlayerInventory(player, "MORTE");
        logPlayerEffects(player, "MORTE");

        String deathCause = getDeathCauseName(cause);
        notifyBotActivity(player, "MORTE", "morreu: " + deathCause);

        logFullInventoryToConsole(player, "MORTE");
        logEffectsToConsole(player, "MORTE");
    }

    private String getDeathCauseName(String cause) {
        switch (cause) {
            case "mob": return "Monstro";
            case "player": return "PvP";
            case "arrow": return "Flecha";
            case "fall": return "Queda";
            case "fire": return "Fogo";
            case "lava": return "Lava";
            case "drown": return "Afogamento";
            case "explosion": return "Explosão";
            case "magic": return "Magia";
            case "wither": return "Wither";
            case "starve": return "Fome";
            default: return cause;
        }
    }

    // ========== SISTEMA DE INVENTÁRIO COMPLETO ==========
    public void logFullPlayerInventory(PlayerEntity player, String situation) {
        try {
            StringBuilder inventoryLog = new StringBuilder();
            inventoryLog.append("INVENTARIO_COMPLETO (").append(situation).append("): ");

            int totalItems = 0;

            // Hotbar (0-8)
            StringBuilder hotbarLog = new StringBuilder();
            for (int i = 0; i < 9; i++) {
                ItemStack item = player.inventory.getItem(i);
                if (!item.isEmpty()) {
                    if (hotbarLog.length() > 0) hotbarLog.append(", ");
                    hotbarLog.append("HOTBAR").append(i+1).append(": ").append(item.getCount()).append("x ").append(getItemName(item));
                    totalItems++;
                }
            }
            if (hotbarLog.length() > 0) {
                inventoryLog.append("HOTBAR[").append(hotbarLog.toString()).append("] ");
            }

            // Inventário principal (9-35)
            StringBuilder mainInvLog = new StringBuilder();
            for (int i = 9; i < 36; i++) {
                ItemStack item = player.inventory.getItem(i);
                if (!item.isEmpty()) {
                    if (mainInvLog.length() > 0) mainInvLog.append(", ");
                    mainInvLog.append("SLOT").append(i-8).append(": ").append(item.getCount()).append("x ").append(getItemName(item));
                    totalItems++;
                }
            }
            if (mainInvLog.length() > 0) {
                inventoryLog.append("INVENTARIO[").append(mainInvLog.toString()).append("] ");
            }

            // Armadura (36-39)
            StringBuilder armorLog = new StringBuilder();
            for (int i = 36; i < 40; i++) {
                ItemStack item = player.inventory.getItem(i);
                if (!item.isEmpty()) {
                    if (armorLog.length() > 0) armorLog.append(", ");
                    armorLog.append(getArmorSlotName(i)).append(": ").append(item.getCount()).append("x ").append(getItemName(item));
                    totalItems++;
                }
            }
            if (armorLog.length() > 0) {
                inventoryLog.append("ARMADURA[").append(armorLog.toString()).append("] ");
            }

            // Mão secundária (40)
            ItemStack offhandItem = player.inventory.getItem(40);
            if (!offhandItem.isEmpty()) {
                inventoryLog.append("OFFHAND: ").append(offhandItem.getCount()).append("x ").append(getItemName(offhandItem)).append(" ");
                totalItems++;
            }

            inventoryLog.append("| TOTAL_ITENS: ").append(totalItems)
                    .append(" | XP: ").append(player.totalExperience)
                    .append(" | NIVEL: ").append(player.experienceLevel);

            logToFile(player, "INVENTARIO", inventoryLog.toString());

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao logar inventário completo: {}", e.toString());
        }
    }

    // ========== SISTEMA DE EFEITOS ==========
    public void logPlayerEffects(PlayerEntity player, String situation) {
        try {
            StringBuilder effectsLog = new StringBuilder();
            effectsLog.append("EFEITOS (").append(situation).append("): ");

            boolean hasEffects = false;

            for (EffectInstance effect : player.getActiveEffects()) {
                if (!hasEffects) hasEffects = true;
                if (effectsLog.length() > 15) effectsLog.append(", ");

                String effectName = getEffectName(effect);
                int amplifier = effect.getAmplifier() + 1;
                int duration = effect.getDuration() / 20;

                effectsLog.append(effectName).append(" Nv.").append(amplifier)
                        .append(" (").append(duration).append("s)");
            }

            if (!hasEffects) {
                effectsLog.append("NENHUM_EFEITO_ATIVO");
            }

            logToFile(player, "EFEITOS", effectsLog.toString());

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao logar efeitos: {}", e.toString());
        }
    }

    // ========== SISTEMA DE LOG NO CONSOLE ==========
    private void logFullInventoryToConsole(PlayerEntity player, String situation) {
        try {
            NexusBotMod.LOGGER.info("🎒 INVENTÁRIO COMPLETO DE {} ({})", player.getName().getString(), situation);

            int totalItems = 0;

            // Hotbar
            NexusBotMod.LOGGER.info("  🔥 HOTBAR (Slots 1-9):");
            boolean hasHotbarItems = false;
            for (int i = 0; i < 9; i++) {
                ItemStack item = player.inventory.getItem(i);
                if (!item.isEmpty()) {
                    NexusBotMod.LOGGER.info("    Slot {}: {}x {}", i+1, item.getCount(), getItemName(item));
                    totalItems++;
                    hasHotbarItems = true;
                }
            }
            if (!hasHotbarItems) {
                NexusBotMod.LOGGER.info("    Vazio");
            }

            // Inventário principal
            NexusBotMod.LOGGER.info("  📦 INVENTÁRIO PRINCIPAL (Slots 10-36):");
            boolean hasMainItems = false;
            for (int i = 9; i < 36; i++) {
                ItemStack item = player.inventory.getItem(i);
                if (!item.isEmpty()) {
                    NexusBotMod.LOGGER.info("    Slot {}: {}x {}", i+1, item.getCount(), getItemName(item));
                    totalItems++;
                    hasMainItems = true;
                }
            }
            if (!hasMainItems) {
                NexusBotMod.LOGGER.info("    Vazio");
            }

            // Armadura
            NexusBotMod.LOGGER.info("  🛡️ ARMADURA:");
            boolean hasArmor = false;
            for (int i = 36; i < 40; i++) {
                ItemStack item = player.inventory.getItem(i);
                if (!item.isEmpty()) {
                    NexusBotMod.LOGGER.info("    {}: {}x {}", getArmorSlotName(i), item.getCount(), getItemName(item));
                    totalItems++;
                    hasArmor = true;
                }
            }
            if (!hasArmor) {
                NexusBotMod.LOGGER.info("    Vazio");
            }

            // Mão secundária
            ItemStack offhandItem = player.inventory.getItem(40);
            if (!offhandItem.isEmpty()) {
                NexusBotMod.LOGGER.info("  ✋ OFFHAND: {}x {}", offhandItem.getCount(), getItemName(offhandItem));
                totalItems++;
            } else {
                NexusBotMod.LOGGER.info("  ✋ OFFHAND: Vazio");
            }

            NexusBotMod.LOGGER.info("  📊 RESUMO: {} itens no total | XP: {} | Nível: {}",
                    totalItems, player.totalExperience, player.experienceLevel);

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao logar inventário completo no console: {}", e.toString());
        }
    }

    private void logEffectsToConsole(PlayerEntity player, String situation) {
        try {
            NexusBotMod.LOGGER.info("⚡ EFEITOS ATIVOS DE {} ({})", player.getName().getString(), situation);

            boolean hasEffects = false;

            for (EffectInstance effect : player.getActiveEffects()) {
                if (!hasEffects) hasEffects = true;

                String effectName = getEffectName(effect);
                int amplifier = effect.getAmplifier() + 1;
                int duration = effect.getDuration() / 20;

                NexusBotMod.LOGGER.info("  💊 {} - Nível {} ({} segundos restantes)", effectName, amplifier, duration);
            }

            if (!hasEffects) {
                NexusBotMod.LOGGER.info("  ✅ Nenhum efeito ativo");
            }

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao logar efeitos no console: {}", e.toString());
        }
    }

    // ========== SISTEMA DE BAÚS COMPLETO ==========
    public void logChestOpen(PlayerEntity player, BlockPos pos, String chestType) {
        String logMsg = String.format("ABRIU %s em %d,%d,%d", chestType, pos.getX(), pos.getY(), pos.getZ());
        logToFile(player, "BAU", logMsg);
        notifyBotActivity(player, "INTERACAO", "abriu " + chestType);
    }

    public void logChestPut(PlayerEntity player, BlockPos pos, ItemStack item, String chestType) {
        String itemInfo = item.getCount() + "x " + getItemName(item);
        String logMsg = String.format("COLOCOU no %s em %d,%d,%d: %s", chestType, pos.getX(), pos.getY(), pos.getZ(), itemInfo);
        logToFile(player, "BAU", logMsg);
        notifyBotActivity(player, "ITEM", "colocou " + itemInfo + " no " + chestType);
    }

    public void logChestTake(PlayerEntity player, BlockPos pos, ItemStack item, String chestType) {
        String itemInfo = item.getCount() + "x " + getItemName(item);
        String logMsg = String.format("TIROU do %s em %d,%d,%d: %s", chestType, pos.getX(), pos.getY(), pos.getZ(), itemInfo);
        logToFile(player, "BAU", logMsg);
        notifyBotActivity(player, "ITEM", "tirou " + itemInfo + " do " + chestType);
    }

    // ========== SISTEMA DE ITENS ==========
    public void logItemDrop(PlayerEntity player, ItemStack item, boolean dropped) {
        String action = dropped ? "DROPOU" : "COLETOU";
        String itemInfo = item.getCount() + "x " + getItemName(item);
        logToFile(player, "ITENS", action + ": " + itemInfo);
        if (dropped) {
            notifyBotActivity(player, "ITEM", "dropou " + itemInfo);
        } else {
            notifyBotActivity(player, "ITEM", "coletou " + itemInfo);
        }
    }

    public void logCrafting(PlayerEntity player, ItemStack item) {
        String itemInfo = item.getCount() + "x " + getItemName(item);
        logToFile(player, "CRAFT", "CRAFTOU: " + itemInfo);
        notifyBotActivity(player, "CRAFT", "crafrou " + itemInfo);
    }

    public void logSmelting(PlayerEntity player, ItemStack item) {
        String itemInfo = item.getCount() + "x " + getItemName(item);
        logToFile(player, "SMELT", "FUNDIU: " + itemInfo);
        notifyBotActivity(player, "CRAFT", "fundiu " + itemInfo);
    }

    // ========== SISTEMA DE MINERAÇÃO E CONSTRUÇÃO ==========
    public void logBlockBreak(PlayerEntity player, BlockPos pos, String blockName) {
        String logMsg = String.format("QUEBROU %s em %d,%d,%d", blockName, pos.getX(), pos.getY(), pos.getZ());
        logToFile(player, "BLOCOS", logMsg);
        if (isValuableBlock(blockName)) {
            notifyBotActivity(player, "MINERACAO", "minerou " + blockName);
        } else {
            notifyBotActivity(player, "CONSTRUCAO", "quebrou " + blockName);
        }
    }

    public void logBlockPlace(PlayerEntity player, BlockPos pos, String blockName) {
        String logMsg = String.format("PLACOU %s em %d,%d,%d", blockName, pos.getX(), pos.getY(), pos.getZ());
        logToFile(player, "BLOCOS", logMsg);
        notifyBotActivity(player, "CONSTRUCAO", "construiu com " + blockName);
    }

    private boolean isValuableBlock(String blockName) {
        return blockName.contains("diamond") || blockName.contains("iron") ||
                blockName.contains("gold") || blockName.contains("emerald") ||
                blockName.contains("redstone") || blockName.contains("lapis") ||
                blockName.contains("quartz") || blockName.contains("ancient") ||
                blockName.contains("netherite");
    }

    // ========== SISTEMA DE MODERAÇÃO ==========
    public void logPunishment(PlayerEntity player, String reason, String punishment) {
        logToFile(player, "MODERACAO", punishment + " - Motivo: " + reason);
        logToFile(player, "MODERACAO", "Posição: " + getPositionString(player));
        notifyBotActivity(player, "MODERACAO", "foi " + punishment.toLowerCase());
    }

    // ========== SISTEMA DE MOVIMENTAÇÃO ==========
    public void logMovement(PlayerEntity player, String from, String to) {
        logToFile(player, "MOVIMENTO", "De: " + from + " Para: " + to);
        if (isLongDistance(from, to)) {
            notifyBotActivity(player, "EXPLORACAO", "está explorando o mundo");
        }
    }

    private boolean isLongDistance(String from, String to) {
        try {
            String[] fromCoords = from.split(",");
            String[] toCoords = to.split(",");
            if (fromCoords.length == 3 && toCoords.length == 3) {
                int fromX = Integer.parseInt(fromCoords[0]);
                int fromZ = Integer.parseInt(fromCoords[2]);
                int toX = Integer.parseInt(toCoords[0]);
                int toZ = Integer.parseInt(toCoords[2]);
                int distance = (int) Math.sqrt(Math.pow(toX - fromX, 2) + Math.pow(toZ - fromZ, 2));
                return distance > 100;
            }
        } catch (Exception e) {}
        return false;
    }

    // ========== SISTEMA DE COMBATE ==========
    public void logEntityAttack(PlayerEntity player, String entityType) {
        logToFile(player, "COMBATE", "Atacou: " + entityType);
        if (entityType.contains("player")) {
            notifyBotActivity(player, "PVP", "entrou em combate PvP");
        } else {
            notifyBotActivity(player, "COMBATE", "atacou " + entityType);
        }
    }

    public void logItemUse(PlayerEntity player, ItemStack item) {
        String itemInfo = item.getCount() + "x " + getItemName(item);
        logToFile(player, "ITENS", "USOU: " + itemInfo);
        if (isSpecialItem(item)) {
            notifyBotActivity(player, "ITEM", "usou " + itemInfo);
        }
    }

    private boolean isSpecialItem(ItemStack item) {
        String itemName = getItemName(item).toLowerCase();
        return itemName.contains("potion") || itemName.contains("enchanted") ||
                itemName.contains("ender") || itemName.contains("nether") ||
                itemName.contains("totem") || itemName.contains("crystal");
    }

    // ========== SISTEMA DE EXPERIÊNCIA ==========
    public void logExperience(PlayerEntity player, int levels, int experience) {
        logToFile(player, "SISTEMA", "Ganhou " + experience + " XP e " + levels + " níveis");
        if (levels > 0 || experience > 100) {
            notifyBotActivity(player, "XP", "ganhou " + experience + " XP");
        }
    }

    // ========== SISTEMA DE TICKETS COMPLETO ==========
    public void logTicketCreate(PlayerEntity player, String message, int ticketId) {
        String logMsg = String.format("CRIOU ticket #%d: %s", ticketId, message);
        logToFile(player, "TICKET", logMsg);
        notifyBotActivity(player, "TICKET", "criou ticket #" + ticketId);
    }

    public void logTicketResponse(PlayerEntity staff, PlayerEntity target, int ticketId, String response) {
        String logMsgStaff = String.format("RESPONDEU ticket #%d para %s: %s", ticketId, target.getName().getString(), response);
        String logMsgTarget = String.format("RECEBEU resposta ticket #%d de %s: %s", ticketId, staff.getName().getString(), response);

        logToFile(staff, "TICKET", logMsgStaff);
        logToFile(target, "TICKET", logMsgTarget);

        notifyBotActivity(staff, "TICKET", "respondeu ticket #" + ticketId);
        notifyBotActivity(target, "TICKET", "recebeu resposta do ticket #" + ticketId);

    }

    public void logTicketStatus(PlayerEntity player, int ticketId, String oldStatus, String newStatus) {
        String logMsg = String.format("ALTEROU status ticket #%d: %s -> %s", ticketId, oldStatus, newStatus);
        logToFile(player, "TICKET", logMsg);
        notifyBotActivity(player, "TICKET", "alterou status do ticket #" + ticketId + " para " + newStatus);
    }

    public void logTicketClose(PlayerEntity player, int ticketId, String reason) {
        String logMsg = String.format("FECHOU ticket #%d: %s", ticketId, reason);
        logToFile(player, "TICKET", logMsg);
        notifyBotActivity(player, "TICKET", "fechou ticket #" + ticketId);
    }

    public void logTicketAccept(PlayerEntity staff, int ticketId, String playerName) {
        String logMsg = String.format("ACEITOU ticket #%d de %s", ticketId, playerName);
        logToFile(staff, "TICKET", logMsg);
        notifyBotActivity(staff, "TICKET", "aceitou ticket #" + ticketId + " de " + playerName);
    }

    // ========== SISTEMA DE TELETRANSPORTE ==========
    public void logTeleport(PlayerEntity player, String from, String to) {
        logToFile(player, "TELETRANSPORTE", "De: " + from + " Para: " + to);
        notifyBotActivity(player, "TELETRANSPORTE", "teleportou para novas coordenadas");
    }

    public void logDimensionChange(PlayerEntity player, String fromDimension, String toDimension) {
        logToFile(player, "SISTEMA", "Mudou de dimensão: " + fromDimension + " → " + toDimension);
        notifyBotActivity(player, "EXPLORACAO", "mudou para dimensão " + toDimension);
    }

    // ========== SISTEMA DE VILAGER ==========
    public void logVillagerTrade(PlayerEntity player, String villagerType, ItemStack given, ItemStack received) {
        String givenInfo = given.getCount() + "x " + getItemName(given);
        String receivedInfo = received.getCount() + "x " + getItemName(received);
        String logMsg = String.format("TROCOU com %s: %s -> %s", villagerType, givenInfo, receivedInfo);
        logToFile(player, "TRADE", logMsg);
        notifyBotActivity(player, "TRADE", "trocou com " + villagerType);
    }

    // ========== MÉTODOS AUXILIARES ==========
    private String getPositionString(PlayerEntity player) {
        BlockPos pos = player.blockPosition();
        return String.format("%d,%d,%d", pos.getX(), pos.getY(), pos.getZ());
    }

    private String getArmorSlotName(int slot) {
        switch (slot) {
            case 36: return "HELMET";
            case 37: return "CHESTPLATE";
            case 38: return "LEGGINGS";
            case 39: return "BOOTS";
            default: return "ARMOR" + (slot - 35);
        }
    }

    private String getItemName(ItemStack item) {
        try {
            if (item.hasCustomHoverName()) {
                return item.getHoverName().getString() + " [" + item.getItem().getRegistryName().getPath() + "]";
            } else {
                return item.getHoverName().getString();
            }
        } catch (Exception e) {
            return "ItemDesconhecido";
        }
    }

    private String getEffectName(EffectInstance effect) {
        try {
            ResourceLocation effectId = effect.getEffect().getRegistryName();
            if (effectId != null) {
                String path = effectId.getPath();
                switch (path) {
                    case "speed": return "Velocidade";
                    case "slowness": return "Lentidão";
                    case "haste": return "Pressa";
                    case "mining_fatigue": return "Fadiga";
                    case "strength": return "Força";
                    case "instant_health": return "Cura Instantânea";
                    case "instant_damage": return "Dano Instantâneo";
                    case "jump_boost": return "Salto";
                    case "nausea": return "Náusea";
                    case "regeneration": return "Regeneração";
                    case "resistance": return "Resistência";
                    case "fire_resistance": return "Resistência ao Fogo";
                    case "water_breathing": return "Respiração Aquática";
                    case "invisibility": return "Invisibilidade";
                    case "blindness": return "Cegueira";
                    case "night_vision": return "Visão Noturna";
                    case "hunger": return "Fome";
                    case "weakness": return "Fraqueza";
                    case "poison": return "Veneno";
                    case "wither": return "Wither";
                    case "health_boost": return "Vida Extra";
                    case "absorption": return "Absorção";
                    case "saturation": return "Saturação";
                    case "glowing": return "Brilho";
                    case "levitation": return "Levitação";
                    case "luck": return "Sorte";
                    case "unluck": return "Má Sorte";
                    case "slow_falling": return "Queda Lenta";
                    case "conduit_power": return "Poder do Conduto";
                    case "dolphins_grace": return "Graça do Golfinho";
                    case "bad_omen": return "Mau Presságio";
                    case "hero_of_the_village": return "Herói da Vila";
                    default: return effect.getEffect().getDisplayName().getString();
                }
            }
            return effect.getEffect().getDisplayName().getString();
        } catch (Exception e) {
            return "EfeitoDesconhecido";
        }
    }

    public void cleanup() {
        for (PrintWriter writer : playerWriters.values()) {
            if (writer != null) {
                writer.close();
            }
        }
        playerWriters.clear();
    }

    public void logTicket(PlayerEntity player, String s) {

    }
}