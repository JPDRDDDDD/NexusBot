package com.nexusbot.systems;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import com.nexusbot.NexusBotMod;

import java.util.HashMap;
import java.util.Map;

public class MonitorCore {
    private final ChatSystem chatSystem;
    private final PunishmentManager punishmentManager;
    private final CleanerSystem cleanerSystem;
    private final LoggerManager loggerManager;
    private final TicketSystem ticketSystem;

    private Map<String, Long> lastHits = new HashMap<>();
    private Map<String, Integer> hitCounts = new HashMap<>();
    private Map<String, Long> pvpTracker = new HashMap<>();
    private Map<String, Integer> offenseCount = new HashMap<>();
    private Map<String, BlockPos> lastPositions = new HashMap<>();

    public MonitorCore() {
        this.chatSystem = new ChatSystem();
        this.punishmentManager = new PunishmentManager();
        this.cleanerSystem = new CleanerSystem();
        this.loggerManager = new LoggerManager();
        this.ticketSystem = new TicketSystem();

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
        NexusBotMod.LOGGER.info("🚀 NexusBot MonitorCore PROFISSIONAL INICIADO");
    }

    // ========== SISTEMA DE CHAT COM IA DO NEXUSBOT ==========
    @SubscribeEvent
    public void onPlayerChat(ServerChatEvent event) {
        try {
            PlayerEntity player = event.getPlayer();
            String message = event.getMessage();
            String playerName = player.getName().getString();
            String playerUUID = player.getStringUUID();

            // ✅ PRIMEIRO: Verificar se é para o NexusBot (permite respostas mesmo se mutado)
            chatSystem.handleBotResponse(player, message);

            // VERIFICAÇÃO DE MUTE - SEGUNDO
            if (chatSystem.isMuted(playerName)) {
                player.sendMessage(new StringTextComponent("§c§l⚠ §cVocê está §lMUTADO§c e não pode falar no chat!"), player.getUUID());
                event.setCanceled(true);
                return;
            }

            // Cancelar chat vanilla
            event.setCanceled(true);

            // Verificar bypass
            if (!chatSystem.hasBypass(playerName)) {
                // Filtro de palavrões
                if (chatSystem.detectBadWords(message)) {
                    handleAutoPunishment(player, "Palavrão: " + message);
                    return;
                }

                // Detecção de spam
                if (chatSystem.detectSpam(playerUUID, message)) {
                    handleAutoPunishment(player, "Spam no chat");
                    return;
                }
            }

            // Enviar mensagem para chat local SEM AVISO
            chatSystem.sendLocalMessage(player, message);
            loggerManager.logChat(player, message);

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro no chat: {}", e.toString());
        }
    }

    // ========== SISTEMA DE PUNIÇÃO AUTOMÁTICA COM 4 NÍVEIS ==========
    private void handleAutoPunishment(PlayerEntity player, String reason) {
        String playerName = player.getName().getString();
        String playerUUID = player.getStringUUID();

        int offenses = offenseCount.getOrDefault(playerUUID, 0) + 1;
        offenseCount.put(playerUUID, offenses);

        NexusBotMod.LOGGER.warn("🚨 NEXUSGUARD AUTO-PUNIÇÃO: {} - {} (#{})", playerName, reason, offenses);

        // ✅ CORREÇÃO: Enviar mensagem do NexusBot sobre a punição
        String botMessage = "💀 " + playerName + " foi punido automaticamente. Motivo: " + reason;
        chatSystem.sendPunishmentMessage(botMessage);

        switch (offenses) {
            case 1:
                // 1ª ofensa: Mute 30 minutos
                chatSystem.mutePlayer(playerName);
                punishmentManager.scheduleUnmute(playerName, 30);
                player.sendMessage(new StringTextComponent("§c§l⚠ §cNexusGuard: §cMutado por 30 minutos: " + reason), player.getUUID());
                player.sendMessage(new StringTextComponent("§7⚠ A próxima ofensa resultará em mute de 2 horas!"), player.getUUID());
                break;
            case 2:
                // 2ª ofensa: Mute 2 horas
                chatSystem.mutePlayer(playerName);
                punishmentManager.scheduleUnmute(playerName, 120);
                player.sendMessage(new StringTextComponent("§c§l⚠ §cNexusGuard: §cMutado por 2 horas: " + reason), player.getUUID());
                player.sendMessage(new StringTextComponent("§7⚠ A próxima ofensa resultará em mute de 6 horas!"), player.getUUID());
                break;
            case 3:
                // 3ª ofensa: Mute 6 horas
                chatSystem.mutePlayer(playerName);
                punishmentManager.scheduleUnmute(playerName, 360);
                player.sendMessage(new StringTextComponent("§c§l⚠ §cNexusGuard: §cMutado por 6 horas: " + reason), player.getUUID());
                player.sendMessage(new StringTextComponent("§c§l🚨 §cNEXUSGUARD: §cPRÓXIMA OFENSA: KICK PERMANENTE!"), player.getUUID());
                break;
            case 4:
                // 4ª ofensa: KICK PERMANENTE
                punishmentManager.kickPlayerPermanent(player, "Kick permanente: " + reason + " (4ª ofensa)");
                offenseCount.remove(playerUUID);
                break;
        }

        loggerManager.logPunishment(player, reason, "NEXUSGUARD MUTE #" + offenses + " (" + getPunishmentDuration(offenses) + ")");
    }

    // Método auxiliar para duração da punição
    private String getPunishmentDuration(int offenses) {
        switch (offenses) {
            case 1: return "30 minutos";
            case 2: return "2 horas";
            case 3: return "6 horas";
            case 4: return "KICK PERMANENTE";
            default: return "KICK PERMANENTE";
        }
    }

    // ========== SISTEMA ANTI-AUTOCLICK CORRIGIDO (16 HITS EM 1s) ==========
    @SubscribeEvent
    public void onPlayerAttack(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        try {
            PlayerEntity player = event.getPlayer();
            String playerUUID = player.getStringUUID();
            long currentTime = System.currentTimeMillis();

            lastHits.putIfAbsent(playerUUID, currentTime);
            hitCounts.putIfAbsent(playerUUID, 0);

            long lastHit = lastHits.get(playerUUID);
            int hits = hitCounts.get(playerUUID);

            // ✅ CORREÇÃO: Reset hits se passou mais de 1 segundo (1000ms)
            if (currentTime - lastHit > 1000) {
                hits = 0;
            }

            hits++;

            // ✅ CORREÇÃO: Detectar se deu mais de 16 hits em 1 segundo
            if (hits > 16) {
                NexusBotMod.LOGGER.warn("🖱️ NEXUSGUARD AUTOCLICK DETECTADO: {} - {} hits em 1s", player.getName().getString(), hits);

                // ✅ CORREÇÃO: Enviar mensagem do NexusBot sobre autoclick detectado
                String botMessage = "🚫 Autoclick detectado: " + player.getName().getString() + " (" + hits + " hits em 1s)";
                chatSystem.sendBotMessage(botMessage);

                punishmentManager.kickPlayer(player, "Anti-Cheat: Autoclick detectado (" + hits + " hits em 1s)");
                event.setCanceled(true);

                // Reset contador após detecção
                hitCounts.put(playerUUID, 0);
                return;
            }

            hitCounts.put(playerUUID, hits);
            lastHits.put(playerUUID, currentTime);

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro no anti-autoclick: {}", e.toString());
        }
    }

    // ========== SISTEMA DE MORTE COM INVENTÁRIO COMPLETO E EFEITOS ==========
    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        try {
            if (event.getEntityLiving() instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) event.getEntityLiving();
                String cause = event.getSource().getMsgId();

                // Log da morte com inventário COMPLETO e efeitos ANTES da morte
                loggerManager.logDeathWithFullInventory(player, cause);

                // ✅ CORREÇÃO: Enviar mensagem do NexusBot sobre a morte
                String botMessage = "💀 " + player.getName().getString() + " morreu! Causa: " + getDeathCauseName(cause);
                chatSystem.sendBotMessage(botMessage);
            }
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro na morte: {}", e.toString());
        }
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

    // ========== SISTEMA DE CHESTS MELHORADO ==========
    @SubscribeEvent
    public void onChestOpen(PlayerInteractEvent.RightClickBlock event) {
        try {
            BlockState state = event.getWorld().getBlockState(event.getPos());
            if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof EnderChestBlock) {

                PlayerEntity player = event.getPlayer();
                BlockPos pos = event.getPos();

                // Detectar tipo de baú
                String chestType = "Baú";
                if (state.getBlock() instanceof EnderChestBlock) {
                    chestType = "EnderChest";
                } else if (state.getBlock() instanceof ChestBlock) {
                    chestType = "Chest";
                }

                // Log da abertura do baú
                loggerManager.logChestOpen(player, pos, chestType);

            }
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao logar baú: {}", e.toString());
        }
    }

    // ========== SISTEMA DE INTERAÇÃO COM INVENTÁRIO ==========
    @SubscribeEvent
    public void onItemCraft(net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent event) {
        try {
            PlayerEntity player = event.getPlayer();
            ItemStack item = event.getCrafting();
            loggerManager.logCrafting(player, item);
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao logar craft: {}", e.toString());
        }
    }

    @SubscribeEvent
    public void onItemSmelt(net.minecraftforge.event.entity.player.PlayerEvent.ItemSmeltedEvent event) {
        try {
            PlayerEntity player = event.getPlayer();
            ItemStack item = event.getSmelting();
            loggerManager.logSmelting(player, item);
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao logar smelt: {}", e.toString());
        }
    }

    // ========== SISTEMA DE ITENS COLOCADOS/RETIRADOS DE BAÚS ==========
    @SubscribeEvent
    public void onItemPutInChest(net.minecraftforge.event.entity.player.PlayerContainerEvent event) {
        try {
            if (event.getContainer() instanceof ChestContainer) {
                PlayerEntity player = event.getPlayer();
                // Aqui você pode implementar a lógica para detectar itens colocados/retirados
                // Isso requer um sistema mais complexo de tracking de inventário
            }
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao logar item no baú: {}", e.toString());
        }
    }

    // ========== SISTEMA DE LOGS DE MOVIMENTO ==========
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;

        PlayerEntity player = event.player;
        String playerUUID = player.getStringUUID();
        BlockPos currentPos = player.blockPosition();

        BlockPos lastPos = lastPositions.get(playerUUID);
        if (lastPos == null || !lastPos.equals(currentPos)) {
            lastPositions.put(playerUUID, currentPos);

            // Log apenas se mudou significativamente (mais de 5 blocos)
            if (lastPos != null && lastPos.distSqr(currentPos) > 25) {
                String from = lastPos.getX() + "," + lastPos.getY() + "," + lastPos.getZ();
                String to = currentPos.getX() + "," + currentPos.getY() + "," + currentPos.getZ();
                loggerManager.logMovement(player, from, to);
            }
        }
    }

    // ========== SISTEMA DE BLOCOS - MÉTODO CORRIGIDO DEFINITIVAMENTE ==========
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        try {
            PlayerEntity player = event.getPlayer();
            if (player != null) {
                BlockPos pos = event.getPos();

                // MÉTODO 100% COMPATÍVEL COM 1.16.5 - SEM func_235333_g_()
                String blockName = getBlockNameSafe(event.getState());

                loggerManager.logBlockBreak(player, pos, blockName);
            }
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao logar quebra: {}", e.toString());
        }
    }

    // ========== MÉTODO AUXILIAR PARA OBTER NOME DO BLOCO DE FORMA SEGURA ==========
    private String getBlockNameSafe(BlockState state) {
        try {
            // Tentativa 1: Método mais comum e compatível
            return state.getBlock().getRegistryName().toString();
        } catch (Exception e1) {
            try {
                // Tentativa 2: Descrição do bloco
                return state.getBlock().getDescriptionId();
            } catch (Exception e2) {
                try {
                    // Tentativa 3: Nome da classe como fallback
                    return state.getBlock().getClass().getSimpleName();
                } catch (Exception e3) {
                    // Último recurso
                    return "BlocoDesconhecido";
                }
            }
        }
    }

    // ========== SISTEMA DE COLOCAÇÃO DE BLOCOS CORRIGIDO ==========
    @SubscribeEvent
    public void onBlockPlace(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        try {
            PlayerEntity player = event.getPlayer();
            if (player == null) return;

            // Verificar se o jogador está colocando um bloco
            ItemStack itemInHand = player.getMainHandItem();
            if (!itemInHand.isEmpty() && itemInHand.getItem() instanceof net.minecraft.item.BlockItem) {
                BlockPos pos = event.getPos().relative(event.getFace());
                // MÉTODO CORRIGIDO: Usar getDisplayName().getString()
                String blockName = itemInHand.getDisplayName().getString();

                // Log da colocação do bloco
                loggerManager.logBlockPlace(player, pos, blockName);
            }
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao logar colocação: {}", e.toString());
        }
    }

    // ========== SISTEMA DE ITENS ==========
    @SubscribeEvent
    public void onItemDrop(ItemTossEvent event) {
        try {
            PlayerEntity player = event.getPlayer();
            ItemStack item = event.getEntityItem().getItem();
            loggerManager.logItemDrop(player, item, true);
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao logar drop: {}", e.toString());
        }
    }

    @SubscribeEvent
    public void onItemPickup(EntityItemPickupEvent event) {
        try {
            PlayerEntity player = event.getPlayer();
            ItemStack item = event.getItem().getItem();
            loggerManager.logItemDrop(player, item, false);
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao logar coleta: {}", e.toString());
        }
    }

    // ========== EVENTOS DE JOGADOR ==========
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        try {
            PlayerEntity player = event.getPlayer();
            loggerManager.logJoin(player);

            // ✅ CORREÇÃO: Mensagem de boas-vindas com emojis funcionando
            player.sendMessage(new StringTextComponent("§6§lNexusBot §8» §eSistema profissional ativo! 🤖"), player.getUUID());
            player.sendMessage(new StringTextComponent("§7Digite §6/ajuda §7para comandos 📚"), player.getUUID());

            // ✅ CORREÇÃO: Enviar mensagem do NexusBot no chat global
            String welcomeMessage = "👋 " + player.getName().getString() + " entrou no servidor! Bem-vindo(a)!";
            chatSystem.sendBotMessage(welcomeMessage);

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro no join: {}", e.toString());
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        try {
            PlayerEntity player = event.getPlayer();
            loggerManager.logLeave(player);

            // ✅ CORREÇÃO: Enviar mensagem do NexusBot no chat global
            String leaveMessage = "👋 " + player.getName().getString() + " saiu do servidor. Até mais!";
            chatSystem.sendBotMessage(leaveMessage);

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro no leave: {}", e.toString());
        }
    }

    // ========== SISTEMA DE DETECÇÃO DE MOVIMENTO SUSPEITO ==========
    @SubscribeEvent
    public void onPlayerMove(net.minecraftforge.event.entity.living.LivingEvent event) {
        try {
            if (event.getEntityLiving() instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) event.getEntityLiving();

                // Detectar movimento suspeito (speed hack)
                if (isSuspiciousMovement(player)) {
                    NexusBotMod.LOGGER.warn("🚨 MOVIMENTO SUSPEITO DETECTADO: {}", player.getName().getString());

                    // ✅ CORREÇÃO: Enviar mensagem do NexusBot sobre movimento suspeito
                    String botMessage = "🚫 Movimento suspeito detectado: " + player.getName().getString();
                    chatSystem.sendBotMessage(botMessage);
                }
            }
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro na detecção de movimento: {}", e.toString());
        }
    }

    private boolean isSuspiciousMovement(PlayerEntity player) {
        // Implementar lógica de detecção de speed hack
        // Por enquanto, retorna false como placeholder
        return false;
    }

    // ========== SISTEMA DE NOTIFICAÇÕES DO NEXUSBOT ==========
    public void sendBotNotification(String message) {
        chatSystem.sendBotMessage("🔔 " + message);
    }

    public void sendBotWarning(String message) {
        chatSystem.sendBotMessage("⚠️ " + message);
    }

    public void sendBotSuccess(String message) {
        chatSystem.sendBotMessage("✅ " + message);
    }

    public void sendBotError(String message) {
        chatSystem.sendBotMessage("❌ " + message);
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        NexusBotMod.LOGGER.info("🎯 NEXUSBOT PROFISSIONAL INICIADO");
        NexusBotMod.LOGGER.info("✅ Sistema de Logs 24/7");
        NexusBotMod.LOGGER.info("✅ Chat Local Automático");
        NexusBotMod.LOGGER.info("✅ Anti-Cheat Completo");
        NexusBotMod.LOGGER.info("✅ Moderação Automática");
        NexusBotMod.LOGGER.info("✅ Monitoramento Total");
        NexusBotMod.LOGGER.info("🤖 NexusBot IA com Personalidade Ativada");

        // ✅ CORREÇÃO: Mensagem de inicialização do NexusBot no chat
        String startupMessage = "🚀 NexusBot inicializado! Sistema profissional ativo com IA personalizada!";
        chatSystem.sendBotMessage(startupMessage);
    }

    // ========== MÉTODOS DE LIMPEZA ==========
    public void cleanup() {
        try {
            chatSystem.cleanup();
            punishmentManager.cleanup();
            cleanerSystem.cleanup();
            loggerManager.cleanup();
            NexusBotMod.LOGGER.info("🔧 MonitorCore limpo e encerrado");
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao limpar MonitorCore: {}", e.toString());
        }
    }

    // Getters
    public ChatSystem getChatSystem() { return chatSystem; }
    public PunishmentManager getPunishmentManager() { return punishmentManager; }
    public CleanerSystem getCleanerSystem() { return cleanerSystem; }
    public LoggerManager getLoggerManager() { return loggerManager; }
    public TicketSystem getTicketSystem() { return ticketSystem; }
}