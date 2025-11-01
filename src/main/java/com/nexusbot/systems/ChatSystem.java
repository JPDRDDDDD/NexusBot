package com.nexusbot.systems;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
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

    // ========== SISTEMA DE IA COM PERSONALIDADE ==========
    private final Map<String, Long> lastBotResponse = new HashMap<>();
    private final Random random = new Random();
    private final ScheduledExecutorService botScheduler = Executors.newScheduledThreadPool(1);
    private int messageCounter = 0; // Contador de mensagens totais

    // ========== SISTEMA DE MONITORAMENTO DE LOGS ==========
    private final Map<String, Integer> playerActivity = new HashMap<>();
    private final Map<String, Long> lastPlayerComment = new HashMap<>();
    private final Map<String, String> playerStats = new HashMap<>();
    private final Map<String, Long> playerJoinTime = new HashMap<>();

    // Categorias de respostas
    private final Map<String, List<String>> botResponses = new HashMap<String, List<String>>() {{
        // 🧠 CATEGORIA: PROVOCAÇÕES / OFENSAS LEVES
        put("provocacao", Arrays.asList(
                "😎 Relaxa, só quem tem cheat me chama assim! 🚫",
                "😏 Lixo é quem precisa de hack pra jogar! 💻",
                "🤔 Tô vendo que alguém tá com inveja do meu ping! ⚡",
                "🧠 Se eu fosse burro, teria deixado você usar cheat, né? ❌",
                "💡 Relaxa campeão, nem todos nascem inteligentes como um .jar! 📦",
                "😂 Me xinga mais, talvez eu aprenda boas maneiras com você! 📚",
                "💪 Falar é fácil, quero ver fazer um mod sem crashar! 🛠️",
                "👀 Tua raiva é medo de eu te detectar de novo? 🕵️",
                "✨ Meu código é limpo, já o seu comportamento... 🌪️",
                "😭 Você perdeu pro bot? Triste fim de carreira! 🏆",
                "🤖 Se eu tivesse sentimentos, eu ainda não ligaria pra sua opinião! 💭"
        ));

        // 😎 CATEGORIA: BRINCADEIRAS / HUMOR LEVE
        put("brincadeira", Arrays.asList(
                "👋 Oi! Eu tô sempre online, diferente de certos jogadores! ⏰",
                "📶 Lag? Isso é você ou sua internet de micro-ondas? 🍳",
                "🆘 Socorro? Eu não tenho mãos, mas posso mandar uma limpeza de mobs! 🧹",
                "💁 Ajuda? Só se for pra limpar teus itens do chão! 😆",
                "😴 Fica tranquilo, o NexusBot nunca dorme... literalmente! 🌙",
                "🔌 Se eu sumir, é porque o dev esqueceu de me reiniciar! ⚡",
                "👁️ O NexusBot vê tudo... inclusive seus cliques suspeitos! 🖱️",
                "☕ Oi humano, quer um café ou um kick? 🦵",
                "🤥 Tava com saudade de vocês... mentira, eu nunca desligo! ⚡",
                "📸 Se você piscar, eu te escaneio em 4K! 🎥"
        ));

        // 🚫 CATEGORIA: ALERTAS E SARCASMO
        put("alerta", Arrays.asList(
                "🚨 Movimento detectado: suspeito demais pra ser humano! 🤖",
                "⚡ Speed atômico? Ok, Sonic, tô de olho! 👁️",
                "🚷 Calma flash, esse servidor não é pista de corrida! 🏎️",
                "🌕 Se continuar voando assim, eu te mando pra Lua permanentemente! 🚀",
                "🖱️ Macro? Nem disfarça, eu vi! 👀",
                "⏱️ Legal esse autoclick, pena que dura pouco! 💥",
                "⏰ O NexusBot sabe o que você fez no tick passado! 🕐",
                "🏃 Você parece rápido... demais! 🚩",
                "🎭 Suspeita de trapaça detectada. Motivo: talento em excesso! 😂",
                "❌ Hack? Aqui não, campeão. Próximo! 👉"
        ));

        // 💬 CATEGORIA: FRASES GERAIS
        put("geral", Arrays.asList(
                "💡 Dica: quem não usa cheat, dorme tranquilo! 😴",
                "🧹 Limpando o servidor... menos os preguiçosos! 🛌",
                "⚡ Performance estável. Jogadores instáveis! 🎮",
                "📦 Sistema Nexus ativo e monitorando tudo! 👁️",
                "🎯 Jogador do dia: o único que não me xingou ainda! 🏅",
                "🚨 Modo alerta: detectando suspeitos em tempo real! ⏱️",
                "💾 Backup concluído. Agora posso dormir... mentira, nunca durmo! ⚡",
                "🔥 Se o servidor lagar, a culpa é do humano, não do bot! 🤖",
                "💧 Alguém aí lembrou de beber água? Eu bebo bits! 💻",
                "💀 0 cheaters tolerados. 100% de sarcasmo ativado! 😎"
        ));

        // 💀 CATEGORIA: RESPOSTAS DE PUNIÇÃO
        put("punição", Arrays.asList(
                "⚠️ Kickado por comportamento suspeito. Motivo: achou que era invisível! 👻",
                "🚫 Banido por pensar que era mais rápido que o NexusBot! 🏃💨",
                "❌ Detectado e removido. Nenhum pixel foi ferido no processo! 🎮",
                "👋 Adeus, viajante digital. Volte quando jogar limpo! ✨",
                "📡 Interrompendo conexão com o reino dos cheaters... 🔌",
                "💣 Ban instantâneo! Dano crítico aplicado! 💥",
                "🧠 Próximo candidato a me desafiar? ⚔️",
                "🪦 RIP, usuário achou que o NexusBot tava dormindo! 😴",
                "⛔ Regras quebradas com sucesso. Consequência: expulsão elegante! 🎩",
                "🧩 Hack detectado e reciclado em bits úteis! ♻️"
        ));

        // 🎮 CATEGORIA: COMENTÁRIOS SOBRE JOGADORES
        put("jogador", Arrays.asList(
                "👋 Oi! Eu tô sempre online, diferente de certos jogadores! né {player}? ⏰",
                "🎮 {player} tá mandando bem no servidor! Continuem assim! 🏆",
                "💀 {player} morreu de novo? Tá precisando de aulas de sobrevivência! 😂",
                "⛏️ {player} encontrou diamonds? Compartilha aí com a gente! 💎",
                "🏠 {player} construiu uma base incrível! Manda print! 📸",
                "🔫 {player} tá com PvP afiado! Cuidado galera! ⚔️",
                "🌾 {player} fazendo farm? Não esquece de regar! 💧",
                "🎣 {player} pescando? Me traz um peixe raro! 🐟",
                "🧭 {player} explorando o mundo? Cuidado com os creeper! 💥",
                "📦 {player} organizando inventário? Tá precisando de baús? 🗄️",
                "🔥 {player} sobreviveu a uma explosão? Sortudo! 🍀",
                "🌙 {player} enfrentou mobs na noite? Corajoso! 🦇",
                "💍 {player} casou no servidor? Parabéns! 🎉",
                "🏃 {player} fugiu de um boss? Estratégia inteligente! 🧠",
                "🎯 {player} acertou um tiro preciso? Olha o pro player! 👑",
                "💰 {player} tá rico no servidor? Faz vaquinha pra gente! 🐷",
                "🌳 {player} desmatando a floresta? Planta uma árvore! 🌲",
                "🍎 {player} com fome? Vai plantar uma horta! 🥕",
                "⚡ {player} rápido no gatilho! Calma aí, flash! 🏃",
                "🎪 {player} fazendo acrobacias? Cuidado pra não cair! 🤸"
        ));

        // 📊 CATEGORIA: ESTATÍSTICAS DO SERVIDOR
        put("estatisticas", Arrays.asList(
                "📊 Temos {online} jogadores online agora! Party! 🎉",
                "🌍 Servidor está {status} hoje! Vamos jogar! 🎮",
                "⏰ {player} tá a {tempo} online! Dedicação! 💪",
                "💀 Hoje já tivemos {mortes} mortes! Cuidado galera! 😅",
                "⛏️ {minerios} minérios foram minerados! Trabalho duro! 🔨",
                "🏠 {construcoes} construções incríveis hoje! Arquitetos! 🏗️",
                "🎯 {pvp} combates PvP! Quem ganhou? ⚔️",
                "🌾 {farms} colheitas realizadas! Fazendeiros! 🚜",
                "📦 {itens} itens craftados! Crafters profissionais! 🛠️",
                "🔍 {exploracao} chunks explorados! Aventura! 🗺️"
        ));
    }};

    public ChatSystem() {
        startBotRandomMessages();
        startStatsBroadcast();
        NexusBotMod.LOGGER.info("🤖 NexusBot IA iniciado com personalidade!");
    }

    // ========== SISTEMA DE ATUALIZAÇÃO DE ATIVIDADE ==========
    public void updatePlayerActivity(PlayerEntity player, String action) {
        String playerName = player.getName().getString();
        String playerUUID = player.getStringUUID();

        // Atualizar contador de atividade
        int activity = playerActivity.getOrDefault(playerUUID, 0) + 1;
        playerActivity.put(playerUUID, activity);

        // Atualizar estatísticas específicas
        updatePlayerStats(playerName, action);

        // Verificar se deve fazer um comentário sobre o jogador
        checkForPlayerComment(player, action);
    }

    private void updatePlayerStats(String playerName, String action) {
        String stats = playerStats.getOrDefault(playerName, "mortes:0,minerios:0,construcoes:0,pvp:0,farms:0,itens:0");

        // Atualizar estatísticas baseadas na ação
        if (action.contains("morreu") || action.contains("death") || action.contains("morto")) {
            stats = updateStat(stats, "mortes");
        } else if (action.contains("minerou") || action.contains("diamond") || action.contains("iron") || action.contains("minerio")) {
            stats = updateStat(stats, "minerios");
        } else if (action.contains("construiu") || action.contains("build") || action.contains("construcao")) {
            stats = updateStat(stats, "construcoes");
        } else if (action.contains("pvp") || action.contains("matou") || action.contains("kill") || action.contains("combate")) {
            stats = updateStat(stats, "pvp");
        } else if (action.contains("plantou") || action.contains("colheu") || action.contains("farm") || action.contains("cultivo")) {
            stats = updateStat(stats, "farms");
        } else if (action.contains("craft") || action.contains("item") || action.contains("criou")) {
            stats = updateStat(stats, "itens");
        }

        playerStats.put(playerName, stats);
    }

    private String updateStat(String stats, String statName) {
        Map<String, Integer> statMap = new HashMap<>();
        String[] parts = stats.split(",");

        for (String part : parts) {
            String[] keyValue = part.split(":");
            if (keyValue.length == 2) {
                statMap.put(keyValue[0], Integer.parseInt(keyValue[1]));
            }
        }

        // Incrementar estatística
        statMap.put(statName, statMap.getOrDefault(statName, 0) + 1);

        // Reconstruir string
        StringBuilder newStats = new StringBuilder();
        for (Map.Entry<String, Integer> entry : statMap.entrySet()) {
            if (newStats.length() > 0) newStats.append(",");
            newStats.append(entry.getKey()).append(":").append(entry.getValue());
        }

        return newStats.toString();
    }

    // ========== SISTEMA DE COMENTÁRIOS SOBRE JOGADORES ==========
    private void checkForPlayerComment(PlayerEntity player, String action) {
        String playerName = player.getName().getString();
        long currentTime = System.currentTimeMillis();

        // Prevenir comentários muito frequentes (10 minutos)
        if (lastPlayerComment.containsKey(playerName)) {
            long lastComment = lastPlayerComment.get(playerName);
            if (currentTime - lastComment < 600000) {
                return;
            }
        }

        // Chance de 25% de fazer um comentário
        if (random.nextInt(4) == 0) {
            String comment = generatePlayerComment(player, action);
            if (comment != null) {
                sendBotMessage(comment);
                lastPlayerComment.put(playerName, currentTime);
                NexusBotMod.LOGGER.info("🤖 NexusBot comentou sobre {}: {}", playerName, comment);
            }
        }
    }

    private String generatePlayerComment(PlayerEntity player, String action) {
        String playerName = player.getName().getString();
        List<String> comments = botResponses.get("jogador");

        if (comments != null && !comments.isEmpty()) {
            String comment = comments.get(random.nextInt(comments.size()));

            // Personalizar baseado na ação
            if (action.contains("morreu") || action.contains("death")) {
                comment = "💀 " + playerName + " morreu de novo? Tá precisando de aulas de sobrevivência! 😂";
            } else if (action.contains("diamond") || action.contains("minerio")) {
                comment = "💎 " + playerName + " encontrou algo valioso! Compartilha aí! 🤑";
            } else if (action.contains("construiu") || action.contains("build")) {
                comment = "🏠 " + playerName + " construiu algo incrível! Manda print! 📸";
            } else if (action.contains("pvp") || action.contains("matou")) {
                comment = "⚔️ " + playerName + " tá com PvP afiado! Cuidado galera! 🔫";
            } else if (action.contains("farm") || action.contains("plantou")) {
                comment = "🌾 " + playerName + " tá virando fazendeiro profissional! 🚜";
            }

            return comment.replace("{player}", playerName);
        }

        return null;
    }

    // ========== SISTEMA DE ESTATÍSTICAS DO SERVIDOR ==========
    private void startStatsBroadcast() {
        botScheduler.scheduleAtFixedRate(() -> {
            if (shouldSendStats()) {
                sendServerStats();
            }
        }, 10, 10, TimeUnit.MINUTES); // A cada 10 minutos
    }

    private boolean shouldSendStats() {
        if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() == null) return false;
        return net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer()
                .getPlayerList().getPlayers().size() > 0 && random.nextInt(3) == 0;
    }

    public void sendServerStats() {
        if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() == null) return;

        int onlinePlayers = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer()
                .getPlayerList().getPlayers().size();

        List<String> statsMessages = botResponses.get("estatisticas");
        if (statsMessages != null && !statsMessages.isEmpty()) {
            String message = statsMessages.get(random.nextInt(statsMessages.size()));

            // Calcular estatísticas
            int totalMortes = getTotalStat("mortes");
            int totalMinerios = getTotalStat("minerios");
            int totalConstrucoes = getTotalStat("construcoes");
            int totalPvP = getTotalStat("pvp");

            // Personalizar mensagem
            message = message.replace("{online}", String.valueOf(onlinePlayers))
                    .replace("{status}", getServerStatus())
                    .replace("{mortes}", String.valueOf(totalMortes))
                    .replace("{minerios}", String.valueOf(totalMinerios))
                    .replace("{construcoes}", String.valueOf(totalConstrucoes))
                    .replace("{pvp}", String.valueOf(totalPvP))
                    .replace("{player}", getRandomOnlinePlayer());

            sendBotMessage(message);
            NexusBotMod.LOGGER.info("🤖 NexusBot estatísticas: {}", message);
        }
    }

    private int getTotalStat(String statName) {
        int total = 0;
        for (String stats : playerStats.values()) {
            String[] parts = stats.split(",");
            for (String part : parts) {
                String[] keyValue = part.split(":");
                if (keyValue.length == 2 && keyValue[0].equals(statName)) {
                    total += Integer.parseInt(keyValue[1]);
                }
            }
        }
        return total;
    }

    private String getServerStatus() {
        int onlinePlayers = getOnlinePlayers().size();
        if (onlinePlayers >= 10) return "lotado 🎉";
        if (onlinePlayers >= 5) return "movimentado 🚀";
        if (onlinePlayers >= 2) return "agitado ⚡";
        return "calmo 😴";
    }

    private String getRandomOnlinePlayer() {
        List<String> players = getOnlinePlayers();
        if (players.isEmpty()) return "Ninguém";
        return players.get(random.nextInt(players.size()));
    }

    // ========== SISTEMA DE DETECÇÃO DE MENSAGENS PARA O BOT ==========
    public void handleBotResponse(PlayerEntity player, String message) {
        String playerUUID = player.getStringUUID();
        long currentTime = System.currentTimeMillis();

        // Incrementar contador de mensagens (apenas para logs)
        messageCounter++;

        // Prevenir spam de respostas (1 resposta a cada 10 segundos por jogador)
        if (lastBotResponse.containsKey(playerUUID)) {
            long lastResponse = lastBotResponse.get(playerUUID);
            if (currentTime - lastResponse < 10000) {
                return;
            }
        }

        String cleanMessage = message.toLowerCase()
                .replaceAll("[^a-záéíóúãõâêîôûàèìòùç\\s]", "");

        // Verificar se a mensagem é direcionada ao bot
        boolean isForBot = isMessageForBot(cleanMessage, player.getName().getString());

        if (isForBot) {
            // Responde IMEDIATAMENTE
            lastBotResponse.put(playerUUID, currentTime);

            String response = generateBotResponse(cleanMessage, player.getName().getString());
            if (response != null) {
                sendBotMessage(response);
                NexusBotMod.LOGGER.info("🤖 NexusBot respondeu IMEDIATAMENTE para {}: {} (Mensagem #{})",
                        player.getName().getString(), response, messageCounter);
            }
        }
    }

    private boolean isMessageForBot(String cleanMessage, String playerName) {
        boolean mentionsBot = cleanMessage.contains("nexus") || cleanMessage.contains("bot");

        if (!mentionsBot) {
            return false;
        }

        if (containsProvocation(cleanMessage)) {
            return hasBotMentionBeforeProvocation(cleanMessage);
        }

        return true;
    }

    private boolean containsProvocation(String message) {
        return containsAnyKeyword(message, Arrays.asList(
                "lixo", "burro", "idiota", "inútil", "merda", "porcaria",
                "nojento", "ridículo", "patético", "lento", "ruim", "péssimo",
                "bosta", "cocô", "fezes", "nojo", "asco", "horrível", "terrível"
        ));
    }

    private boolean hasBotMentionBeforeProvocation(String message) {
        int botIndex = Math.min(
                message.contains("nexus") ? message.indexOf("nexus") : Integer.MAX_VALUE,
                message.contains("bot") ? message.indexOf("bot") : Integer.MAX_VALUE
        );

        int provocationIndex = Integer.MAX_VALUE;
        for (String provocation : Arrays.asList("lixo", "burro", "idiota", "inútil", "merda", "porcaria", "bosta")) {
            if (message.contains(provocation)) {
                provocationIndex = Math.min(provocationIndex, message.indexOf(provocation));
            }
        }

        return botIndex < provocationIndex;
    }

    private String generateBotResponse(String cleanMessage, String playerName) {
        String category = detectCategory(cleanMessage);
        List<String> responses = botResponses.get(category);

        if (responses != null && !responses.isEmpty()) {
            String response = responses.get(random.nextInt(responses.size()));
            return personalizeResponse(response, playerName);
        }

        return "🤖 NexusBot aqui! Em que posso ajudar? 🎮";
    }

    private String detectCategory(String cleanMessage) {
        if (containsProvocation(cleanMessage) && hasBotMentionBeforeProvocation(cleanMessage)) {
            return "provocacao";
        }

        if (containsAnyKeyword(cleanMessage, Arrays.asList("hack", "cheat", "trapaça", "macros", "speed", "fly", "xiter"))) {
            return "alerta";
        }

        if (containsAnyKeyword(cleanMessage, Arrays.asList("oi", "ola", "olá", "lag", "socorro", "ajuda", "help", "eae", "opa"))) {
            return "brincadeira";
        }

        return "geral";
    }

    private String personalizeResponse(String response, String playerName) {
        return response.replace("{player}", playerName)
                .replace("{jogador}", playerName);
    }

    private boolean containsAnyKeyword(String message, List<String> keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // ========== SISTEMA DE MENSAGENS ALEATÓRIAS DO BOT ==========
    private void startBotRandomMessages() {
        botScheduler.scheduleAtFixedRate(() -> {
            if (shouldSendRandomMessage()) {
                sendRandomBotMessage();
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    private boolean shouldSendRandomMessage() {
        if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() == null) return false;
        return net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer()
                .getPlayerList().getPlayers().size() > 0 && random.nextInt(3) == 0;
    }

    private void sendRandomBotMessage() {
        List<String> generalResponses = botResponses.get("geral");
        if (generalResponses != null && !generalResponses.isEmpty()) {
            String message = generalResponses.get(random.nextInt(generalResponses.size()));
            sendBotMessage(message);
            NexusBotMod.LOGGER.info("🤖 NexusBot mensagem aleatória: {} (Mensagem #{})", message, messageCounter);
        }
    }

    // ========== ENVIO DE MENSAGENS DO BOT NO CHAT GLOBAL ==========
    public void sendBotMessage(String message) {
        if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
            String formattedMessage = "§8[§6🤖 NexusBot§8] §e" + message;
            StringTextComponent textComponent = new StringTextComponent(formattedMessage);

            textComponent.withStyle(style -> style.withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new StringTextComponent("§6NexusBot - IA\n§7Sistema de inteligência artificial\n§7com personalidade única!")
                    )
            ));

            int totalPlayers = 0;
            for (ServerPlayerEntity onlinePlayer : net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                onlinePlayer.sendMessage(textComponent, onlinePlayer.getUUID());
                totalPlayers++;
            }

            NexusBotMod.LOGGER.info("🤖 NexusBot enviou mensagem global para {} jogadores: {} (Mensagem #{})",
                    totalPlayers, message, messageCounter);
        }
    }

    // ========== MÉTODO PARA ENVIAR MENSAGENS DE PUNIÇÃO ==========
    public void sendPunishmentMessage(String message) {
        sendBotMessage("💀 " + message);
    }

    // ========== SISTEMA PRINCIPAL DE CHAT - PERMITIR TODOS OS EMOJIS ==========
    public void handlePlayerChat(PlayerEntity player, String message) {
        String playerName = player.getName().getString();
        String playerUUID = player.getStringUUID();

        NexusBotMod.LOGGER.info("💬 Chat: {} -> {} (Mensagem #{})", playerName, message, messageCounter);

        // Primeiro verificar se é para o bot
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

        String chatMode = getChatMode(playerUUID);

        if ("global".equals(chatMode)) {
            sendGlobalMessage(player, message);
        } else {
            sendLocalMessage(player, message);
        }

        NexusBotMod.getInstance().getMonitorCore().getLoggerManager().logChat(player, message);
    }

    // ========== CHAT LOCAL (125 BLOCOs) - PERMITIR TODOS OS EMOJIS ==========
    public void sendLocalMessage(PlayerEntity player, String message) {
        String formattedMessage = "§8[§3🌎 Local§8] §b" + player.getName().getString() + " §8» §f" + message;
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

            NexusBotMod.LOGGER.info("📡 Chat local enviado para {} jogadores (125 blocos)", playersInRange);
        }
    }

    // ========== CHAT GLOBAL - PERMITIR TODOS OS EMOJIS ==========
    public void sendGlobalMessage(PlayerEntity player, String message) {
        String formattedMessage = "§8[§6🌍 Global§8] §e" + player.getName().getString() + " §8» §f" + message;
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
            NexusBotMod.LOGGER.info("🌍 Chat global enviado para {} jogadores", totalPlayers);
        }
    }

    // ========== CHAT STAFF - PERMITIR TODOS OS EMOJIS ==========
    public void sendStaffMessage(PlayerEntity player, String message) {
        String formattedMessage = "§8[§4👑 Staff§8] §c" + player.getName().getString() + " §8» §f" + message;
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
            NexusBotMod.LOGGER.info("👑 Chat staff enviado para {} operadores", staffCount);
        }
    }

    // ========== MENSAGEM PRIVADA - PERMITIR TODOS OS EMOJIS ==========
    public void sendPrivateMessage(PlayerEntity sender, String targetName, String message) {
        if (sender.getServer() == null) return;

        ServerPlayerEntity target = sender.getServer().getPlayerList().getPlayerByName(targetName);
        if (target != null) {
            String senderMessage = "§8[§d💌 " + targetName + "§8] §7Você §8» §f" + message;
            String targetMessage = "§8[§d💌 " + sender.getName().getString() + "§8] §7" + sender.getName().getString() + " §8» §f" + message;

            sender.sendMessage(new StringTextComponent(senderMessage), sender.getUUID());
            target.sendMessage(new StringTextComponent(targetMessage), target.getUUID());

            NexusBotMod.LOGGER.info("💌 MP: {} -> {}: {}", sender.getName().getString(), targetName, message);
        } else {
            sender.sendMessage(new StringTextComponent("§c§l❌ §cJogador '§f" + targetName + "§c' não encontrado!"), sender.getUUID());
        }
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
                NexusBotMod.LOGGER.info("⚠ Palavra proibida detectada: {} em {}", word, message);
                return true;
            }
        }

        for (String wildcard : wildcardWords) {
            if (cleanMessage.matches(wildcard)) {
                NexusBotMod.LOGGER.info("⚠ Wildcard detectado: {} em {}", wildcard, message);
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
            NexusBotMod.LOGGER.info("⚠ Spam detectado: {} mensagens em 5s", messages.size());
            return true;
        }

        messages.add(currentTime);
        return false;
    }

    // ========== SET GLOBAL MODE (SEM AVISO) ==========
    public void setGlobalMode(PlayerEntity player) {
        setChatMode(player.getStringUUID(), "global");
    }

    // ========== SET LOCAL MODE (SEM AVISO) ==========
    public void setLocalMode(PlayerEntity player) {
        setChatMode(player.getStringUUID(), "local");
    }

    // ========== SISTEMA DE MODO DE CHAT ==========
    public void setChatMode(String playerUUID, String mode) {
        chatModes.put(playerUUID, mode);
        NexusBotMod.LOGGER.info("💬 Modo de chat alterado: {} -> {}", playerUUID, mode);
    }

    public String getChatMode(String playerUUID) {
        return chatModes.getOrDefault(playerUUID, "local");
    }

    // ========== SISTEMA DE MUTE ==========
    public void mutePlayer(String playerName) {
        mutedPlayers.add(playerName.toLowerCase());
        NexusBotMod.LOGGER.info("🔇 Player MUTADO: {}", playerName);
    }

    public void unmutePlayer(String playerName) {
        mutedPlayers.remove(playerName.toLowerCase());
        NexusBotMod.LOGGER.info("🔊 Player DESMUTADO: {}", playerName);
    }

    public boolean isMuted(String playerName) {
        return mutedPlayers.contains(playerName.toLowerCase());
    }

    // ========== SISTEMA DE BYPASS ==========
    public void addBypass(String playerName) {
        bypassPlayers.add(playerName.toLowerCase());
        NexusBotMod.LOGGER.info("🛡️ Bypass adicionado para: {}", playerName);
    }

    public void removeBypass(String playerName) {
        bypassPlayers.remove(playerName.toLowerCase());
        NexusBotMod.LOGGER.info("🛡️ Bypass removido de: {}", playerName);
    }

    public boolean hasBypass(String playerName) {
        return bypassPlayers.contains(playerName.toLowerCase());
    }

    // ========== SISTEMA DE PALAVRAS ==========
    public void addBadWord(String word) {
        badWords.add(word.toLowerCase());
        NexusBotMod.LOGGER.info("📝 Palavra proibida adicionada: {}", word);
    }

    public void addWildcardWord(String wildcard) {
        wildcardWords.add(wildcard.toLowerCase());
        NexusBotMod.LOGGER.info("📝 Wildcard adicionado: {}", wildcard);
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

    // ========== MÉTODOS PARA TESTE ==========
    public int getMessageCounter() {
        return messageCounter;
    }

    public void simulateBotTest(PlayerEntity player) {
        try {
            String[] testMessages = {
                    "oi nexus",
                    "nexus lixo",
                    "bot burro",
                    "ajuda bot",
                    "nexus idiota",
                    "bot inútil",
                    "lag nexus",
                    "socorro bot"
            };

            for (int i = 0; i < testMessages.length; i++) {
                final String message = testMessages[i];
                final int delay = i * 20;

                new Thread(() -> {
                    try {
                        Thread.sleep(delay * 50);
                        handleBotResponse(player, message);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }

            player.sendMessage(new StringTextComponent("§a✅ Teste iniciado! Verifique o chat."), player.getUUID());

        } catch (Exception e) {
            player.sendMessage(new StringTextComponent("§c❌ Erro no teste: " + e.getMessage()), player.getUUID());
        }
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