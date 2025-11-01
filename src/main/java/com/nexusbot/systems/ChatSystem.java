package com.nexusbot.systems;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.server.ServerWorld;
import com.nexusbot.NexusBotMod;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatSystem {
    private Set<String> badWords = new HashSet<>(Arrays.asList(
            "palavrao1", "palavrao2", "insulto1", "insulto2", "caralho", "porra", "foda", "puta", "viado"
    ));
    private Set<String> wildcardWords = new HashSet<>();

    private Map<String, List<Long>> playerMessages = new HashMap<>();
    private Set<String> bypassPlayers = new HashSet<>();
    private Set<String> mutedPlayers = new HashSet<>();
    private Map<String, String> chatModes = new HashMap<>();
    private Map<String, String> customEvents = new HashMap<>();
    private Map<String, String> lastTellTarget = new HashMap<>();

    private final ScheduledExecutorService botScheduler = Executors.newScheduledThreadPool(1);
    private final Random random = new Random();

    // ========== SISTEMA DE IA SIMPLIFICADO ==========
    private final Map<String, Long> lastBotResponse = new HashMap<>();

    public ChatSystem() {
        NexusBotMod.LOGGER.info("Sistema de Chat iniciado");
    }

    // ========== MÉTODOS FALTANTES PARA O MONITORCORE ==========

    /**
     * Método para o MonitorCore - Sistema de IA simplificado
     */
    public void handleBotResponse(PlayerEntity player, String message) {
        String playerUUID = player.getStringUUID();
        long currentTime = System.currentTimeMillis();

        // Evita resposta muito rápida
        if (lastBotResponse.containsKey(playerUUID)) {
            long lastResponse = lastBotResponse.get(playerUUID);
            if (currentTime - lastResponse < 10000) {
                return;
            }
        }

        String cleanMessage = message.toLowerCase();

        // Responde apenas se mencionar o bot especificamente
        if (cleanMessage.contains("nexus") || cleanMessage.contains("bot")) {
            String response = generateSimpleBotResponse(player.getName().getString());
            if (response != null) {
                sendBotMessage(response);
                lastBotResponse.put(playerUUID, currentTime);
            }
        }
    }

    private String generateSimpleBotResponse(String playerName) {
        String[] responses = {
                playerName + ", estou aqui! Em que posso ajudar?",
                "Oi " + playerName + "! NexusBot online e funcionando!",
                playerName + ", sistema de monitoramento ativo!",
                "Olá " + playerName + "! Tudo bem por ai?"
        };
        return responses[random.nextInt(responses.length)];
    }

    /**
     * Método para o MonitorCore - Enviar mensagens de punição
     */
    public void sendPunishmentMessage(String message) {
        sendBotMessage("⚖️ " + message);
    }

    /**
     * Método para o LoggerManager - Atualizar atividade do player
     */
    public void updatePlayerActivity(PlayerEntity player, String action) {
        String playerName = player.getName().getString();
        String playerUUID = player.getStringUUID();

        // Log simples da atividade
        NexusBotMod.LOGGER.info("Atividade detectada: {} - {}", playerName, action);
    }

    // ========== SISTEMA DE MENSAGENS DO BOT PARA EVENTOS ==========

    public void onPlayerJoin(PlayerEntity player) {
        String playerName = player.getName().getString();
        String welcomeMessage = "👋 " + playerName + " entrou no servidor! Bem-vindo(a)!";
        sendBotMessage(welcomeMessage);
    }

    public void onPlayerLeave(PlayerEntity player) {
        String playerName = player.getName().getString();
        String leaveMessage = "👋 " + playerName + " saiu do servidor. Até mais!";
        sendBotMessage(leaveMessage);
    }

    public void onPlayerAdvancement(PlayerEntity player, String advancementName) {
        String playerName = player.getName().getString();

        // Obter mensagem personalizada
        String message = getAdvancementMessage(playerName, advancementName);
        if (message != null) {
            sendBotMessage(message);
        }

        NexusBotMod.LOGGER.info("🎯 Conquista: {} -> {}", playerName, advancementName);
    }

    // ========== SISTEMA DE MENSAGENS PARA CONQUISTAS ==========
    public String getAdvancementMessage(String playerName, String advancementName) {
        // Primeiro verifica se tem evento customizado
        String customMessage = customEvents.get(advancementName.toLowerCase());
        if (customMessage != null) {
            return translateColors(customMessage.replace("{player}", playerName));
        }

        // Mensagens padrão para conquistas específicas
        switch (advancementName.toLowerCase()) {
            // ========== MINECRAFT VANILLA ==========
            case "minecraft:story/mine_stone":
                return "⛏️ " + playerName + " começou sua jornada minerando pedra! Que venham os recursos!";

            case "minecraft:story/mine_diamond":
                return "💎 " + playerName + " encontrou DIAMANTES! Que sorte incrível!";

            case "minecraft:story/enter_the_nether":
                return "🔥 " + playerName + " entrou no Nether! Cuidado com os perigos!";

            case "minecraft:story/enter_the_end":
                return "🌌 " + playerName + " chegou ao Fim! Preparem-se para o dragão!";

            case "minecraft:end/kill_dragon":
                return "🐉 " + playerName + " MATOU O DRAGÃO DO FIM! Lenda viva do servidor!";

            case "minecraft:end/elytra":
                return "🦋 " + playerName + " conseguiu uma Elytra! Hora de voar pelos céus!";

            // ========== DRACONIC EVOLUTION ==========
            case "draconicevolution:wyvern_core":
                return "⚡ " + playerName + " criou um Núcleo Wyvern! Poder draconico adquirido!";

            case "draconicevolution:awakened_core":
                return "🌟 " + playerName + " evoluiu para Núcleo Despertado! Poder cósmico!";

            case "draconicevolution:draconic_core":
                return "🐲 " + playerName + " alcançou o Núcleo Draconico! Poder supremo!";

            case "draconicevolution:chaotic_core":
                return "💥 " + playerName + " dominou o Núcleo Caótico! Poder absoluto!";

            // ========== MEKANISM ==========
            case "mekanism:atomic_disassembler":
                return "🔧 " + playerName + " construiu um Desmontador Atômico! Tecnologia avançada!";

            case "mekanism:mekasuit":
                return "🛡️ " + playerName + " criou a MekaSuit! Proteção máxima ativada!";

            // ========== TINKERS CONSTRUCT ==========
            case "tconstruct:story/melting":
                return "🔥 " + playerName + " dominou a fundição! Hora de criar ferramentas épicas!";

            case "tconstruct:tools/cleaver":
                return "⚔️ " + playerName + " forjou um Cleaver! Lâmina mortal criada!";

            // ========== BOTANIA ==========
            case "botania:main/terrasteel_pickup":
                return "🌿 " + playerName + " criou Terrasteel! Poder da natureza!";

            case "botania:main/gaia_guardian_kill":
                return "👑 " + playerName + " derrotou o Guardião de Gaia! Mestre da Botania!";

            // ========== ARS NOUVEAU ==========
            case "ars_nouveau:novice_spellbook":
                return "📖 " + playerName + " adquiriu um Grimório de Noviço! Magia despertada!";

            case "ars_nouveau:archmage_spellbook":
                return "🔮 " + playerName + " alcançou o Grimório de Arquimago! Poder mágico supremo!";

            // ========== APOTHEOSIS ==========
            case "apotheosis:affix_gear":
                return "✨ " + playerName + " criou equipamento com Afixos! Itens lendários!";

            case "apotheosis:mythic_gear":
                return "🎭 " + playerName + " forjou equipamento Mítico! Poder além do normal!";

            // ========== TWILIGHT FOREST ==========
            case "twilightforest:progress_lich":
                return "🧙 " + playerName + " derrotou o Lich! Coragem na Floresta Twilight!";

            case "twilightforest:progress_ur_ghast":
                return "👻 " + playerName + " venceu o Ur-Ghast! Desbravador das trevas!";

            // ========== BLOOD MAGIC ==========
            case "bloodmagic:altar":
                return "🩸 " + playerName + " construiu um Altar de Sangue! Magia sanguínea ativada!";

            case "bloodmagic:ritual_master":
                return "🌀 " + playerName + " tornou-se Mestre de Rituais! Controle total do sangue!";

            // ========== CREATE ==========
            case "create:water_wheel":
                return "💧 " + playerName + " construiu uma Roda D'água! Energia mecânica criada!";

            case "create:contraption":
                return "⚙️ " + playerName + " dominou as Contrapções! Engenharia criativa!";

            // ========== CYCLIC ==========
            case "cyclic:apple_ender":
                return "🍎 " + playerName + " criou uma Maça do Ender! Teleporte instantâneo!";

            case "cyclic:apple_emerald":
                return "💚 " + playerName + " fez uma Maça de Esmeralda! Fortuna verde!";

            // ========== FORBIDDEN ARCANUS ==========
            case "forbidden_arcanus:obtain_dark_nether_star":
                return "🌑 " + playerName + " obteve uma Estrela do Nether Sombria! Poder proibido!";

            case "forbidden_arcanus:obtain_eternal_stella":
                return "⭐ " + playerName + " conquistou a Eternal Stella! Artefato lendário!";

            // ========== VAMPIRISM ==========
            case "vampirism:become_vampire":
                return "🧛 " + playerName + " tornou-se um Vampiro! Noites eternas começam!";

            case "vampirism:become_hunter":
                return "🏹 " + playerName + " juntou-se aos Caçadores! Justiceiro da noite!";

            // ========== RATS ==========
            case "rats:rat_taming":
                return "🐀 " + playerName + " domou seu primeiro Rato! Amizade roedora!";

            case "rats:rat_upgrade_aristocrat":
                return "👑 " + playerName + " tem um Rato Aristocrata! Elegância roedora!";

            // ========== ALLTHEMODIUM ==========
            case "allthemodium:allthemodium_ingot":
                return "💜 " + playerName + " forjou um lingote de Allthemodium! Metal supremo!";

            case "allthemodium:unobtainium_ingot":
                return "🌈 " + playerName + " criou Unobtainium! Material lendário obtido!";

            // ========== CONQUISTAS GENÉRICAS ==========
            default:
                if (advancementName.contains("diamond") || advancementName.contains("diamante")) {
                    return "💎 " + playerName + " conquistou algo com DIAMANTES! Brilho máximo!";
                }
                else if (advancementName.contains("nether") || advancementName.contains("inferno")) {
                    return "🔥 " + playerName + " explorou o Nether! Coragem nas profundezas!";
                }
                else if (advancementName.contains("end") || advancementName.contains("fim")) {
                    return "🌌 " + playerName + " desbravou o Fim! Aventureiro das estrelas!";
                }
                else if (advancementName.contains("boss") || advancementName.contains("chefe")) {
                    return "👹 " + playerName + " derrotou um boss! Força de verdadeiro herói!";
                }
                else if (advancementName.contains("magic") || advancementName.contains("magia")) {
                    return "🔮 " + playerName + " dominou a magia! Poder arcano liberado!";
                }
                else {
                    // Mensagem genérica para outras conquistas
                    return "🎯 " + playerName + " conquistou: " + formatAdvancementName(advancementName) + "! Parabéns!";
                }
        }
    }

    // ========== SISTEMA DE CORES ==========
    public static String translateColors(String message) {
        if (message == null) return null;
        return message.replace("&", "§");
    }

    private String formatAdvancementName(String advancementName) {
        String formatted = advancementName
                .replace("minecraft:", "")
                .replace(":", " - ")
                .replace("_", " ")
                .replace("/", " - ");

        return capitalizeWords(formatted);
    }

    private String capitalizeWords(String text) {
        String[] words = text.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    // ========== SISTEMA DE EVENTOS CUSTOMIZADOS ==========
    public void addCustomEvent(String advancementId, String message) {
        customEvents.put(advancementId.toLowerCase(), message);
        NexusBotMod.LOGGER.info("Evento customizado adicionado: {} -> {}", advancementId, message);
    }

    public void removeCustomEvent(String advancementId) {
        customEvents.remove(advancementId.toLowerCase());
        NexusBotMod.LOGGER.info("Evento customizado removido: {}", advancementId);
    }

    public Map<String, String> getCustomEvents() {
        return new HashMap<>(customEvents);
    }

    public void showColorCodes(PlayerEntity player) {
        player.sendMessage(new StringTextComponent("§6§l📚 CÓDIGOS DE CORES DISPONÍVEIS:"), player.getUUID());
        player.sendMessage(new StringTextComponent(""), player.getUUID());

        player.sendMessage(new StringTextComponent("§0&0 §8Preto"), player.getUUID());
        player.sendMessage(new StringTextComponent("§1&1 §1Azul Escuro"), player.getUUID());
        player.sendMessage(new StringTextComponent("§2&2 §2Verde Escuro"), player.getUUID());
        player.sendMessage(new StringTextComponent("§3&3 §3Azul Claro"), player.getUUID());
        player.sendMessage(new StringTextComponent("§4&4 §4Vermelho"), player.getUUID());
        player.sendMessage(new StringTextComponent("§5&5 §5Roxo"), player.getUUID());
        player.sendMessage(new StringTextComponent("§6&6 §6Laranja"), player.getUUID());
        player.sendMessage(new StringTextComponent("§7&7 §7Cinza"), player.getUUID());
        player.sendMessage(new StringTextComponent("§8&8 §8Cinza Escuro"), player.getUUID());
        player.sendMessage(new StringTextComponent("§9&9 §9Azul"), player.getUUID());
        player.sendMessage(new StringTextComponent("§a&a §aVerde"), player.getUUID());
        player.sendMessage(new StringTextComponent("§b&b §bAzul Claro"), player.getUUID());
        player.sendMessage(new StringTextComponent("§c&c §cVermelho Claro"), player.getUUID());
        player.sendMessage(new StringTextComponent("§d&d §dRosa"), player.getUUID());
        player.sendMessage(new StringTextComponent("§e&e §eAmarelo"), player.getUUID());
        player.sendMessage(new StringTextComponent("§f&f §fBranco"), player.getUUID());
        player.sendMessage(new StringTextComponent(""), player.getUUID());

        player.sendMessage(new StringTextComponent("§k&k §kTexto Aleatório"), player.getUUID());
        player.sendMessage(new StringTextComponent("§l&l §lNegrito"), player.getUUID());
        player.sendMessage(new StringTextComponent("§m&m §mTachado"), player.getUUID());
        player.sendMessage(new StringTextComponent("§n&n §nSublinhado"), player.getUUID());
        player.sendMessage(new StringTextComponent("§o&o §oItálico"), player.getUUID());
        player.sendMessage(new StringTextComponent("§r&r §rReset"), player.getUUID());
        player.sendMessage(new StringTextComponent(""), player.getUUID());

        player.sendMessage(new StringTextComponent("§7Exemplo: &cParabéns &4você conseguiu &2seu primeiro &bDraconic &3Core &nContinue assim&r!"), player.getUUID());
        player.sendMessage(new StringTextComponent("§7Resultado: §cParabéns §4você conseguiu §2seu primeiro §bDraconic §3Core §nContinue assim§r!"), player.getUUID());
    }

    // ========== ENVIO DE MENSAGENS DO BOT ==========
    public void sendBotMessage(String message) {
        if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
            String formattedMessage = "§8[§6NexusBot§8] §e" + message;
            StringTextComponent textComponent = new StringTextComponent(formattedMessage);

            textComponent.withStyle(style -> style.withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new StringTextComponent("§6NexusBot\n§7Sistema de notificações automáticas")
                    )
            ));

            int totalPlayers = 0;
            for (ServerPlayerEntity onlinePlayer : net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                onlinePlayer.sendMessage(textComponent, onlinePlayer.getUUID());
                totalPlayers++;
            }

            NexusBotMod.LOGGER.info("NexusBot: {}", message);
        }
    }

    // ========== SISTEMA PRINCIPAL DE CHAT ==========
    public void handlePlayerChat(PlayerEntity player, String message) {
        String playerName = player.getName().getString();
        String playerUUID = player.getStringUUID();

        NexusBotMod.LOGGER.info("Chat: {} -> {}", playerName, message);

        // ✅ CHAMADA DO SISTEMA DE IA
        handleBotResponse(player, message);

        if (isMuted(playerName)) {
            player.sendMessage(new StringTextComponent("§c§l⚠ §cVocê está §lMUTADO§c e não pode falar no chat!"), player.getUUID());
            return;
        }

        if (!hasBypass(playerName)) {
            if (detectBadWords(message)) {
                player.sendMessage(new StringTextComponent("§c§l🚫 §cSua mensagem contém palavras proibidas!"), player.getUUID());
                return;
            }

            if (detectSpam(playerUUID, message)) {
                player.sendMessage(new StringTextComponent("§c§l⚠ §cNão faça §lSPAM§c no chat!"), player.getUUID());
                return;
            }
        }

        // ✅ MUDANÇA: Mensagem normal vai para GLOBAL (padrão)
        sendGlobalMessage(player, message);
        NexusBotMod.getInstance().getMonitorCore().getLoggerManager().logChat(player, message);
    }

    // ========== NOVO SISTEMA: /l "mensagem" - ENVIA MENSAGEM LOCAL DIRETA ==========
    public void sendLocalMessageDirect(PlayerEntity player, String message) {
        String playerName = player.getName().getString();
        String playerUUID = player.getStringUUID();

        if (isMuted(playerName)) {
            player.sendMessage(new StringTextComponent("§c§l⚠ §cVocê está §lMUTADO§c e não pode falar no chat!"), player.getUUID());
            return;
        }

        if (!hasBypass(playerName)) {
            if (detectBadWords(message)) {
                player.sendMessage(new StringTextComponent("§c§l🚫 §cSua mensagem contém palavras proibidas!"), player.getUUID());
                return;
            }

            if (detectSpam(playerUUID, message)) {
                player.sendMessage(new StringTextComponent("§c§l⚠ §cNão faça §lSPAM§c no chat!"), player.getUUID());
                return;
            }
        }

        sendLocalMessage(player, message);
        NexusBotMod.getInstance().getMonitorCore().getLoggerManager().logChat(player, "[LOCAL] " + message);
    }

    // ========== NOVO SISTEMA: /g "mensagem" - ENVIA MENSAGEM GLOBAL DIRETA ==========
    public void sendGlobalMessageDirect(PlayerEntity player, String message) {
        String playerName = player.getName().getString();
        String playerUUID = player.getStringUUID();

        if (isMuted(playerName)) {
            player.sendMessage(new StringTextComponent("§c§l⚠ §cVocê está §lMUTADO§c e não pode falar no chat!"), player.getUUID());
            return;
        }

        if (!hasBypass(playerName)) {
            if (detectBadWords(message)) {
                player.sendMessage(new StringTextComponent("§c§l🚫 §cSua mensagem contém palavras proibidas!"), player.getUUID());
                return;
            }

            if (detectSpam(playerUUID, message)) {
                player.sendMessage(new StringTextComponent("§c§l⚠ §cNão faça §lSPAM§c no chat!"), player.getUUID());
                return;
            }
        }

        sendGlobalMessage(player, message);
        NexusBotMod.getInstance().getMonitorCore().getLoggerManager().logChat(player, "[GLOBAL] " + message);
    }

    // ========== CHAT LOCAL (125 BLOCOs) ==========
    public void sendLocalMessage(PlayerEntity player, String message) {
        String formattedMessage = "§8[§3Local§8] §b" + player.getName().getString() + " §8» §f" + message;
        StringTextComponent textComponent = new StringTextComponent(formattedMessage);

        textComponent.withStyle(style -> style.withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new StringTextComponent("§aChat Local\n§7Apenas jogadores em um raio de §b125 blocos§7 veem esta mensagem")
                )
        ));

        if (player.level instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) player.level;
            int playersInRange = 0;

            for (ServerPlayerEntity p : serverWorld.players()) {
                if (p.distanceTo(player) <= 125) {
                    p.sendMessage(textComponent, p.getUUID());
                    playersInRange++;
                }
            }

            NexusBotMod.LOGGER.info("Chat local enviado para {} jogadores (125 blocos)", playersInRange);
        }
    }

    // ========== CHAT GLOBAL ==========
    public void sendGlobalMessage(PlayerEntity player, String message) {
        String formattedMessage = "§8[§6Global§8] §e" + player.getName().getString() + " §8» §f" + message;
        StringTextComponent textComponent = new StringTextComponent(formattedMessage);

        textComponent.withStyle(style -> style.withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new StringTextComponent("§6Chat Global\n§7Todos os jogadores do servidor veem esta mensagem")
                )
        ));

        if (player.getServer() != null) {
            int totalPlayers = 0;
            for (ServerPlayerEntity onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
                onlinePlayer.sendMessage(textComponent, onlinePlayer.getUUID());
                totalPlayers++;
            }
            NexusBotMod.LOGGER.info("Chat global enviado para {} jogadores", totalPlayers);
        }
    }

    // ========== SISTEMA DE MODO DE CHAT ==========
    public void setGlobalMode(PlayerEntity player) {
        setChatMode(player.getStringUUID(), "global");
        player.sendMessage(new StringTextComponent("§6§l🌍 §6Chat Global §lATIVADO§6!"), player.getUUID());
        player.sendMessage(new StringTextComponent("§7Agora §etodas§7 suas mensagens serão enviadas para §etodo o servidor§7."), player.getUUID());
        player.sendMessage(new StringTextComponent("§7Use §b/l §7para voltar ao chat local."), player.getUUID());
    }

    public void setLocalMode(PlayerEntity player) {
        setChatMode(player.getStringUUID(), "local");
        player.sendMessage(new StringTextComponent("§b§l🌎 §bChat Local §lATIVADO§b!"), player.getUUID());
        player.sendMessage(new StringTextComponent("§7Agora suas mensagens serão enviadas apenas para jogadores em um raio de §b125 blocos§7."), player.getUUID());
        player.sendMessage(new StringTextComponent("§7Use §6/g §7para chat global."), player.getUUID());
    }

    public void setChatMode(String playerUUID, String mode) {
        chatModes.put(playerUUID, mode);
        NexusBotMod.LOGGER.info("Modo de chat alterado: {} -> {}", playerUUID, mode);
    }

    public String getChatMode(String playerUUID) {
        return chatModes.getOrDefault(playerUUID, "global"); // Padrão é GLOBAL
    }

    // ========== CHAT STAFF ==========
    public void sendStaffMessage(PlayerEntity player, String message) {
        String formattedMessage = "§8[§4Staff§8] §c" + player.getName().getString() + " §8» §f" + message;
        StringTextComponent textComponent = new StringTextComponent(formattedMessage);

        textComponent.withStyle(style -> style.withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new StringTextComponent("§4Chat da Staff\n§7Apenas jogadores com permissão de §cOP§7 veem esta mensagem")
                )
        ));

        if (player.getServer() != null) {
            int staffCount = 0;
            for (ServerPlayerEntity onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
                if (onlinePlayer.hasPermissions(2)) {
                    onlinePlayer.sendMessage(textComponent, onlinePlayer.getUUID());
                    staffCount++;
                }
            }
            NexusBotMod.LOGGER.info("Chat staff enviado para {} operadores", staffCount);
        }
    }

    // ========== MENSAGEM PRIVADA COM SOM ==========
    public void sendPrivateMessage(PlayerEntity sender, String targetName, String message) {
        if (sender.getServer() == null) return;

        ServerPlayerEntity target = sender.getServer().getPlayerList().getPlayerByName(targetName);
        if (target != null) {
            // Verifica se é mensagem para si mesmo
            if (sender.getName().getString().equals(targetName)) {
                sender.sendMessage(new StringTextComponent("§c§l❌ §cVocê não pode enviar mensagens para si mesmo!"), sender.getUUID());
                return;
            }

            String senderMessage = "§8[§d" + targetName + "§8] §7Você §8» §f" + message;
            String targetMessage = "§8[§d" + sender.getName().getString() + "§8] §7" + sender.getName().getString() + " §8» §f" + message;

            // Envia mensagem para o remetente
            sender.sendMessage(new StringTextComponent(senderMessage), sender.getUUID());

            // Envia mensagem para o destinatário
            target.sendMessage(new StringTextComponent(targetMessage), target.getUUID());

            // ✅ CORREÇÃO: Toca som "Level Up" para o destinatário (Método Correto)
            playLevelUpSound(target);

            NexusBotMod.LOGGER.info("MP: {} -> {}: {}", sender.getName().getString(), targetName, message);

            // Salva último destinatário para sistema de resposta (/r)
            lastTellTarget.put(sender.getName().getString(), targetName);

        } else {
            sender.sendMessage(new StringTextComponent("§c§l❌ §cJogador '§f" + targetName + "§c' não encontrado!"), sender.getUUID());
        }
    }

    // ========== SISTEMA DE SOM CORRIGIDO ==========
    private void playLevelUpSound(ServerPlayerEntity player) {
        try {
            // ✅ CORREÇÃO: Método correto para tocar som para um jogador específico
            // Usando playSound diretamente no jogador com volume alto (2.0F)
            player.playSound(SoundEvents.PLAYER_LEVELUP, 2.0F, 1.0F);

            // Log para debug
            NexusBotMod.LOGGER.info("🔊 Som de Level Up tocado para: {} (Volume: 2.0)", player.getName().getString());

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao tocar som para {}: {}", player.getName().getString(), e.getMessage());
        }
    }

    // ========== SISTEMA DE RESPOSTA RÁPIDA (/r) ==========
    public void sendReplyMessage(PlayerEntity sender, String message) {
        String senderName = sender.getName().getString();
        String lastTargetName = lastTellTarget.get(senderName);

        if (lastTargetName == null) {
            sender.sendMessage(new StringTextComponent("§c§l❌ §cVocê não tem ninguém para responder!"), sender.getUUID());
            return;
        }

        if (sender.getServer() == null) return;

        ServerPlayerEntity target = sender.getServer().getPlayerList().getPlayerByName(lastTargetName);
        if (target == null) {
            sender.sendMessage(new StringTextComponent("§c§l❌ §cO jogador '§f" + lastTargetName + "§c' está offline!"), sender.getUUID());
            return;
        }

        // Reutiliza o método de mensagem privada
        sendPrivateMessage(sender, lastTargetName, message);
    }

    // ========== DETECÇÃO DE PALAVRÕES ==========
    public boolean detectBadWords(String message) {
        String cleanMessage = message.toLowerCase()
                .replaceAll("[^a-z]", "")
                .replaceAll("0", "o")
                .replaceAll("1", "i")
                .replaceAll("3", "e")
                .replaceAll("4", "a")
                .replaceAll("5", "s")
                .replaceAll("7", "t")
                .replaceAll("8", "b")
                .replaceAll("9", "g");

        for (String word : badWords) {
            if (cleanMessage.contains(word)) {
                NexusBotMod.LOGGER.info("Palavra proibida detectada: {} em {}", word, message);
                return true;
            }
        }

        for (String wildcard : wildcardWords) {
            if (cleanMessage.matches(wildcard)) {
                NexusBotMod.LOGGER.info("Wildcard detectado: {} em {}", wildcard, message);
                return true;
            }
        }

        return false;
    }

    // ========== DETECÇÃO DE SPAM ==========
    public boolean detectSpam(String playerUUID, String message) {
        long currentTime = System.currentTimeMillis();
        playerMessages.putIfAbsent(playerUUID, new ArrayList<>());

        List<Long> messages = playerMessages.get(playerUUID);
        messages.removeIf(time -> currentTime - time > 5000);

        if (messages.size() >= 5) {
            NexusBotMod.LOGGER.info("Spam detectado: {} mensagens em 5s", messages.size());
            return true;
        }

        messages.add(currentTime);
        return false;
    }

    // ========== SISTEMA DE MUTE ==========
    public void mutePlayer(String playerName) {
        mutedPlayers.add(playerName.toLowerCase());
        NexusBotMod.LOGGER.info("Player MUTADO: {}", playerName);
    }

    public void unmutePlayer(String playerName) {
        mutedPlayers.remove(playerName.toLowerCase());
        NexusBotMod.LOGGER.info("Player DESMUTADO: {}", playerName);
    }

    public boolean isMuted(String playerName) {
        return mutedPlayers.contains(playerName.toLowerCase());
    }

    // ========== SISTEMA DE BYPASS ==========
    public void addBypass(String playerName) {
        bypassPlayers.add(playerName.toLowerCase());
        NexusBotMod.LOGGER.info("Bypass adicionado para: {}", playerName);
    }

    public void removeBypass(String playerName) {
        bypassPlayers.remove(playerName.toLowerCase());
        NexusBotMod.LOGGER.info("Bypass removido de: {}", playerName);
    }

    public boolean hasBypass(String playerName) {
        return bypassPlayers.contains(playerName.toLowerCase());
    }

    // ========== SISTEMA DE PALAVRAS ==========
    public void addBadWord(String word) {
        badWords.add(word.toLowerCase());
        NexusBotMod.LOGGER.info("Palavra proibida adicionada: {}", word);
    }

    public void addWildcardWord(String wildcard) {
        wildcardWords.add(wildcard.toLowerCase());
        NexusBotMod.LOGGER.info("Wildcard adicionado: {}", wildcard);
    }

    // ========== LISTA DE JOGADORES ONLINE ==========
    public List<String> getOnlinePlayers() {
        List<String> onlinePlayers = new ArrayList<>();
        if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayerEntity player : net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                onlinePlayers.add(player.getName().getString());
            }
        }
        return onlinePlayers;
    }

    public String getOnlinePlayersFormatted() {
        List<String> players = getOnlinePlayers();
        if (players.isEmpty()) {
            return "§cNenhum jogador online";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§aJogadores Online (§e").append(players.size()).append("§a):\n");

        for (int i = 0; i < players.size(); i++) {
            if (i > 0) sb.append("§7, ");
            sb.append("§b").append(players.get(i));
        }

        return sb.toString();
    }

    public Collection<String> getBadWords() {
        return badWords;
    }

    public List<String> getAvailableAdvancements() {
        // Método simplificado - retorna lista vazia
        return new ArrayList<>();
    }

    // ========== LIMPEZA DO SCHEDULER ==========
    public void cleanup() {
        try {
            botScheduler.shutdown();
            if (!botScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                botScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            botScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}