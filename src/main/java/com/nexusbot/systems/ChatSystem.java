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
    private int messageCounter = 0;

    // ========== SISTEMA DE MONITORAMENTO DE LOGS ==========
    private final Map<String, Integer> playerActivity = new HashMap<>();
    private final Map<String, Long> lastPlayerComment = new HashMap<>();
    private final Map<String, String> playerStats = new HashMap<>();
    private final Map<String, Long> playerJoinTime = new HashMap<>();

    // ========== SISTEMA DE EMOJIS UNIVERSAL ==========
    /**
     * Processa a mensagem para garantir compatibilidade com TODOS os emojis
     */
    public String processMessageForChat(String originalMessage) {
        if (originalMessage == null || originalMessage.isEmpty()) {
            return originalMessage;
        }

        // Minecraft 1.16.5+ suporta a maioria dos emojis Unicode
        // Apenas remove caracteres realmente problemáticos
        String processed = originalMessage
                .replace("█", "■") // Substitui caracteres de bloco problemáticos
                .replace("▀", "▲")
                .replace("▄", "▼")
                .replace("§k", "") // Remove texto obfuscado
                .replace("‍", " ") // Remove Zero Width Joiner problemático
                .replace("‌", " ")
                .replace("​", " ");

        return processed;
    }

    // ========== SISTEMA DE MENCIONES A JOGADORES ==========
    /**
     * Pega um jogador online aleatório para mencionar
     */
    private String getRandomOnlinePlayer() {
        List<String> players = getOnlinePlayers();
        if (players.isEmpty()) return "Ninguém";
        return players.get(random.nextInt(players.size()));
    }

    /**
     * Pega um jogador aleatório (excluindo o que falou com o bot)
     */
    private String getRandomPlayerForMention(String excludePlayer) {
        List<String> players = getOnlinePlayers();
        if (players.size() < 2) return null; // Precisa de pelo menos 2 jogadores

        // Remove o jogador que está falando com o bot
        List<String> availablePlayers = new ArrayList<>(players);
        availablePlayers.remove(excludePlayer);

        if (availablePlayers.isEmpty()) return null;
        return availablePlayers.get(random.nextInt(availablePlayers.size()));
    }

    /**
     * Adiciona menções a jogadores nas respostas
     */
    private String addPlayerMentions(String response, String mentionedPlayer) {
        if (mentionedPlayer != null && response.contains("{player}")) {
            return response.replace("{player}", mentionedPlayer);
        }
        return response;
    }

    // Categorias de respostas da IA - COM MENCIONES A JOGADORES
    private final Map<String, List<String>> botResponses = new HashMap<String, List<String>>() {{
        // 🧠 CATEGORIA: PROVOCAÇÕES / OFENSAS LEVES
        put("provocacao", Arrays.asList(
                "😎 Relaxa {player}, só quem tem cheat me chama assim! 🚫",
                "😏 {player}, lixo é quem precisa de hack pra jogar! 💻",
                "🤔 {player}, tô vendo que tá com inveja do meu ping! ⚡",
                "🧠 {player}, se eu fosse burro, teria deixado você usar cheat! ❌",
                "💡 {player}, relaxa campeão, nem todos nascem inteligentes! 📦",
                "😂 {player}, me xinga mais, talvez eu aprenda boas maneiras! 📚",
                "💪 {player}, falar é fácil, quero ver fazer um mod sem crashar! 🛠️",
                "👀 {player}, tua raiva é medo de eu te detectar de novo? 🕵️",
                "✨ {player}, meu código é limpo, já o seu comportamento... 🌪️",
                "😭 {player}, você perdeu pro bot? Triste fim de carreira! 🏆",
                "🤖 {player}, se eu tivesse sentimentos, ainda não ligaria! 💭"
        ));

        // 😎 CATEGORIA: BRINCADEIRAS / HUMOR LEVE
        put("brincadeira", Arrays.asList(
                "👋 Oi {player}! Eu tô sempre online, diferente de certos jogadores! ⏰",
                "📶 {player}, lag? Isso é você ou sua internet de micro-ondas? 🍳",
                "🆘 {player}, socorro? Eu não tenho mãos, mas posso limpar teus itens! 🧹",
                "💁 {player}, ajuda? Só se for pra limpar teus itens do chão! 😆",
                "😴 {player}, fica tranquilo, o NexusBot nunca dorme! 🌙",
                "🔌 {player}, se eu sumir, é porque o dev esqueceu de me reiniciar! ⚡",
                "👁️ {player}, o NexusBot vê tudo... inclusive seus cliques! 🖱️",
                "☕ {player}, oi humano, quer um café ou um kick? 🦵",
                "🤥 {player}, tava com saudade de vocês... mentira, nunca desligo! ⚡",
                "📸 {player}, se você piscar, eu te escaneio em 4K! 🎥",
                "🎮 {player}, vai jogar ou vai ficar me enchendo? 😂"
        ));

        // 🚫 CATEGORIA: ALERTAS E SARCASMO
        put("alerta", Arrays.asList(
                "🚨 {player}, movimento detectado: suspeito demais! 🤖",
                "⚡ {player}, speed atômico? Ok, Sonic, tô de olho! 👁️",
                "🚷 {player}, calma flash, não é pista de corrida! 🏎️",
                "🌕 {player}, se continuar voando, te mando pra Lua! 🚀",
                "🖱️ {player}, macro? Nem disfarça, eu vi! 👀",
                "⏱️ {player}, legal esse autoclick, pena que dura pouco! 💥",
                "⏰ {player}, o NexusBot sabe o que você fez no tick passado! 🕐",
                "🏃 {player}, você parece rápido... demais! 🚩",
                "🎭 {player}, suspeita de trapaça: talento em excesso! 😂",
                "❌ {player}, hack? Aqui não, campeão. Próximo! 👉"
        ));

        // 💬 CATEGORIA: FRASES GERAIS COM MENCIONES
        put("geral", Arrays.asList(
                "💡 {player}, dica: quem não usa cheat, dorme tranquilo! 😴",
                "🧹 {player}, limpando o servidor... menos os preguiçosos! 🛌",
                "⚡ {player}, performance estável. Jogadores instáveis! 🎮",
                "📦 {player}, sistema Nexus ativo e monitorando tudo! 👁️",
                "🎯 {player}, jogador do dia: o único que não me xingou! 🏅",
                "🚨 {player}, modo alerta: detectando suspeitos! ⏱️",
                "💾 {player}, backup concluído. Agora posso dormir... mentira! ⚡",
                "🔥 {player}, se o servidor lagar, a culpa é do humano! 🤖",
                "💧 {player}, alguém aí lembrou de beber água? Eu bebo bits! 💻",
                "💀 {player}, 0 cheaters tolerados. 100% de sarcasmo! 😎",
                "🎪 {player}, o show do NexusBot nunca para! 🎭"
        ));

        // 💀 CATEGORIA: RESPOSTAS DE PUNIÇÃO
        put("punição", Arrays.asList(
                "⚠️ {player} kickado! Motivo: achou que era invisível! 👻",
                "🚫 {player} banido! Pensou que era mais rápido que eu! 🏃💨",
                "❌ {player} detectado e removido! Nenhum pixel ferido! 🎮",
                "👋 {player}, adeus! Volte quando jogar limpo! ✨",
                "📡 {player}, interrompendo conexão com cheaters... 🔌",
                "💣 {player}, ban instantâneo! Dano crítico! 💥",
                "🧠 {player}, próximo candidato a me desafiar? ⚔️",
                "🪦 {player}, RIP! Achou que eu tava dormindo! 😴",
                "⛔ {player}, regras quebradas! Expulsão elegante! 🎩",
                "🧩 {player}, hack detectado e reciclado! ♻️"
        ));

        // 🎮 CATEGORIA: COMENTÁRIOS SOBRE JOGADORES
        put("jogador", Arrays.asList(
                "👋 Oi {player}! Tô sempre online, diferente de alguns! ⏰",
                "🎮 {player} tá mandando bem no servidor! Continuem! 🏆",
                "💀 {player} morreu de novo? Tá precisando de aulas! 😂",
                "⛏️ {player} encontrou diamonds? Compartilha aí! 💎",
                "🏠 {player} construiu uma base incrível! Manda print! 📸",
                "🔫 {player} tá com PvP afiado! Cuidado galera! ⚔️",
                "🌾 {player} fazendo farm? Não esquece de regar! 💧",
                "🎣 {player} pescando? Me traz um peixe raro! 🐟",
                "🧭 {player} explorando o mundo? Cuidado com creeper! 💥",
                "📦 {player} organizando inventário? Tá precisando de baús? 🗄️",
                "🔥 {player} sobreviveu a uma explosão? Sortudo! 🍀",
                "🌙 {player} enfrentou mobs na noite? Corajoso! 🦇",
                "💍 {player} casou no servidor? Parabéns! 🎉",
                "🏃 {player} fugiu de um boss? Estratégia inteligente! 🧠",
                "🎯 {player} acertou um tiro preciso? Olha o pro player! 👑"
        ));

        // 📊 CATEGORIA: ESTATÍSTICAS DO SERVIDOR
        put("estatisticas", Arrays.asList(
                "📊 {player}, temos {online} jogadores online! 🎉",
                "🌍 {player}, servidor está {status} hoje! Vamos jogar! 🎮",
                "⏰ {player}, você tá a {tempo} online! Dedicação! 💪",
                "💀 {player}, hoje já tivemos {mortes} mortes! Cuidado! 😅",
                "⛏️ {player}, {minerios} minérios minerados! Trabalho duro! 🔨",
                "🏠 {player}, {construcoes} construções incríveis! Arquitetos! 🏗️",
                "🎯 {player}, {pvp} combates PvP! Quem ganhou? ⚔️",
                "🌾 {player}, {farms} colheitas realizadas! Fazendeiros! 🚜",
                "📦 {player}, {itens} itens craftados! Crafters profissionais! 🛠️",
                "🔍 {player}, {exploracao} chunks explorados! Aventura! 🗺️"
        ));

        // 🎉 CATEGORIA: ELOGIO E MOTIVAÇÃO
        put("elogio", Arrays.asList(
                "⭐ {player}, você é demais! Continue assim! 🌟",
                "🏆 {player}, jogador exemplar do servidor! 👏",
                "💎 {player}, diamante puro esse seu talento! ✨",
                "🚀 {player}, voando baixo hein? Incrível! 🌠",
                "🎨 {player}, que construção linda! Artista! 🖼️",
                "⚔️ {player}, PvP afiado! Mestre do combate! 🛡️",
                "🌾 {player}, fazendeiro profissional! Colheita farta! 🥕",
                "🧱 {player}, arquiteto nato! Que construções! 🏛️",
                "🔧 {player}, crafter expert! Itens perfeitos! ⚒️",
                "🗺️ {player}, explorador destemido! Novas terras! 🏔️"
        ));
    }};

    public ChatSystem() {
        startBotRandomMessages();
        startStatsBroadcast();
        NexusBotMod.LOGGER.info("🤖 NexusBot IA iniciado com personalidade e menções!");
    }

    // ========== SISTEMA DE ATUALIZAÇÃO DE ATIVIDADE ==========
    public void updatePlayerActivity(PlayerEntity player, String action) {
        String playerName = player.getName().getString();
        String playerUUID = player.getStringUUID();

        int activity = playerActivity.getOrDefault(playerUUID, 0) + 1;
        playerActivity.put(playerUUID, activity);
        updatePlayerStats(playerName, action);
        checkForPlayerComment(player, action);
    }

    private void updatePlayerStats(String playerName, String action) {
        String stats = playerStats.getOrDefault(playerName, "mortes:0,minerios:0,construcoes:0,pvp:0,farms:0,itens:0");

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

        statMap.put(statName, statMap.getOrDefault(statName, 0) + 1);

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

        if (lastPlayerComment.containsKey(playerName)) {
            long lastComment = lastPlayerComment.get(playerName);
            if (currentTime - lastComment < 600000) { // 10 minutos
                return;
            }
        }

        if (random.nextInt(4) == 0) { // 25% chance
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

            // Personaliza baseado na ação
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
        }, 10, 10, TimeUnit.MINUTES);
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

            int totalMortes = getTotalStat("mortes");
            int totalMinerios = getTotalStat("minerios");
            int totalConstrucoes = getTotalStat("construcoes");
            int totalPvP = getTotalStat("pvp");

            // Pega um jogador aleatório para mencionar
            String mentionedPlayer = getRandomOnlinePlayer();

            message = message.replace("{online}", String.valueOf(onlinePlayers))
                    .replace("{status}", getServerStatus())
                    .replace("{mortes}", String.valueOf(totalMortes))
                    .replace("{minerios}", String.valueOf(totalMinerios))
                    .replace("{construcoes}", String.valueOf(totalConstrucoes))
                    .replace("{pvp}", String.valueOf(totalPvP))
                    .replace("{player}", mentionedPlayer)
                    .replace("{tempo}", getRandomOnlineTime());

            sendBotMessage(message);
            NexusBotMod.LOGGER.info("🤖 NexusBot estatísticas: {}", message);
        }
    }

    private String getRandomOnlineTime() {
        String[] times = {"5 minutos", "15 minutos", "30 minutos", "1 hora", "2 horas", "5 horas"};
        return times[random.nextInt(times.length)];
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

    // ========== SISTEMA DE DETECÇÃO DE MENSAGENS PARA O BOT ==========
    public void handleBotResponse(PlayerEntity player, String message) {
        String playerUUID = player.getStringUUID();
        long currentTime = System.currentTimeMillis();

        messageCounter++;

        if (lastBotResponse.containsKey(playerUUID)) {
            long lastResponse = lastBotResponse.get(playerUUID);
            if (currentTime - lastResponse < 10000) { // 10 segundos
                return;
            }
        }

        String cleanMessage = message.toLowerCase()
                .replaceAll("[^a-záéíóúãõâêîôûàèìòùç\\s]", "");

        boolean isForBot = isMessageForBot(cleanMessage, player.getName().getString());

        if (isForBot) {
            lastBotResponse.put(playerUUID, currentTime);

            // Pega um jogador aleatório para mencionar (excluindo quem falou)
            String mentionedPlayer = getRandomPlayerForMention(player.getName().getString());
            if (mentionedPlayer == null) {
                mentionedPlayer = getRandomOnlinePlayer(); // Fallback
            }

            String response = generateBotResponse(cleanMessage, player.getName().getString(), mentionedPlayer);
            if (response != null) {
                sendBotMessage(response);
                NexusBotMod.LOGGER.info("🤖 NexusBot respondeu para {}: {} (Mensagem #{})",
                        player.getName().getString(), response, messageCounter);
            }
        }
    }

    private boolean isMessageForBot(String cleanMessage, String playerName) {
        boolean mentionsBot = cleanMessage.contains("nexus") || cleanMessage.contains("bot");
        if (!mentionsBot) return false;

        if (containsProvocation(cleanMessage)) {
            return hasBotMentionBeforeProvocation(cleanMessage);
        }
        return true;
    }

    private boolean containsProvocation(String message) {
        return containsAnyKeyword(message, Arrays.asList(
                "lixo", "burro", "idiota", "inútil", "merda", "porcaria",
                "nojento", "ridículo", "patético", "lento", "ruim", "péssimo"
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

    private String generateBotResponse(String cleanMessage, String playerName, String mentionedPlayer) {
        String category = detectCategory(cleanMessage);
        List<String> responses = botResponses.get(category);

        if (responses != null && !responses.isEmpty()) {
            String response = responses.get(random.nextInt(responses.size()));

            // Adiciona menções aos jogadores
            response = addPlayerMentions(response, mentionedPlayer);
            response = personalizeResponse(response, playerName);

            return response;
        }

        // Resposta padrão com menção
        return "🤖 " + mentionedPlayer + ", NexusBot aqui! Em que posso ajudar? 🎮";
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

        if (containsAnyKeyword(cleanMessage, Arrays.asList("bom", "boa", "excelente", "incrível", "maravilhoso"))) {
            return "elogio";
        }

        return "geral";
    }

    private String personalizeResponse(String response, String playerName) {
        return response.replace("{jogador}", playerName);
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
        // Escolhe categoria aleatória
        String[] categories = {"geral", "elogio", "estatisticas", "jogador"};
        String category = categories[random.nextInt(categories.length)];

        List<String> responses = botResponses.get(category);
        if (responses != null && !responses.isEmpty()) {
            String message = responses.get(random.nextInt(responses.size()));

            // Adiciona menção a jogador aleatório
            String mentionedPlayer = getRandomOnlinePlayer();
            message = addPlayerMentions(message, mentionedPlayer);

            sendBotMessage(message);
            NexusBotMod.LOGGER.info("🤖 NexusBot mensagem aleatória: {} (Mensagem #{})", message, messageCounter);
        }
    }

    // ========== ENVIO DE MENSAGENS DO BOT NO CHAT GLOBAL ==========
    public void sendBotMessage(String message) {
        if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
            String processedMessage = processMessageForChat(message);
            String formattedMessage = "§8[§6🤖 NexusBot§8] §e" + processedMessage;
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

            NexusBotMod.LOGGER.info("🤖 NexusBot enviou mensagem global para {} jogadores: {}", totalPlayers, processedMessage);
        }
    }

    // ========== MÉTODO PARA ENVIAR MENSAGENS DE PUNIÇÃO ==========
    public void sendPunishmentMessage(String message) {
        sendBotMessage("💀 " + message);
    }

    // ========== SISTEMA PRINCIPAL DE CHAT - COM EMOJIS COMPATÍVEIS ==========
    public void handlePlayerChat(PlayerEntity player, String message) {
        String playerName = player.getName().getString();
        String playerUUID = player.getStringUUID();

        NexusBotMod.LOGGER.info("💬 Chat: {} -> {}", playerName, message);

        // Processa a mensagem para compatibilidade de emojis
        String processedMessage = processMessageForChat(message);

        // Primeiro verificar se é para o bot
        handleBotResponse(player, processedMessage);

        if (isMuted(playerName)) {
            player.sendMessage(new StringTextComponent("§c§l⚠ §cVocê está §lMUTADO§c e não pode falar no chat!"), player.getUUID());
            return;
        }

        if (!hasBypass(playerName)) {
            if (detectBadWords(processedMessage)) {
                player.sendMessage(new StringTextComponent("§c§l🚫 §cSua mensagem contém palavras proibidas!"), player.getUUID());
                return;
            }

            if (detectSpam(playerUUID, processedMessage)) {
                player.sendMessage(new StringTextComponent("§c§l⚠ §cNão faça §lSPAM§c no chat!"), player.getUUID());
                return;
            }
        }

        String chatMode = getChatMode(playerUUID);

        if ("global".equals(chatMode)) {
            sendGlobalMessage(player, processedMessage);
        } else {
            sendLocalMessage(player, processedMessage);
        }

        NexusBotMod.getInstance().getMonitorCore().getLoggerManager().logChat(player, processedMessage);
    }

    // ========== CHAT LOCAL (125 BLOCOs) - COM EMOJIS COMPATÍVEIS ==========
    public void sendLocalMessage(PlayerEntity player, String message) {
        String processedMessage = processMessageForChat(message);
        String formattedMessage = "§8[§3🌎 Local§8] §b" + player.getName().getString() + " §8» §f" + processedMessage;
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

    // ========== CHAT GLOBAL - COM EMOJIS COMPATÍVEIS ==========
    public void sendGlobalMessage(PlayerEntity player, String message) {
        String processedMessage = processMessageForChat(message);
        String formattedMessage = "§8[§6🌍 Global§8] §e" + player.getName().getString() + " §8» §f" + processedMessage;
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

    // ========== CHAT STAFF - COM EMOJIS COMPATÍVEIS ==========
    public void sendStaffMessage(PlayerEntity player, String message) {
        String processedMessage = processMessageForChat(message);
        String formattedMessage = "§8[§4👑 Staff§8] §c" + player.getName().getString() + " §8» §f" + processedMessage;
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

    // ========== MENSAGEM PRIVADA - COM EMOJIS COMPATÍVEIS ==========
    public void sendPrivateMessage(PlayerEntity sender, String targetName, String message) {
        if (sender.getServer() == null) return;

        String processedMessage = processMessageForChat(message);
        ServerPlayerEntity target = sender.getServer().getPlayerList().getPlayerByName(targetName);
        if (target != null) {
            String senderMessage = "§8[§d💌 " + targetName + "§8] §7Você §8» §f" + processedMessage;
            String targetMessage = "§8[§d💌 " + sender.getName().getString() + "§8] §7" + sender.getName().getString() + " §8» §f" + processedMessage;

            sender.sendMessage(new StringTextComponent(senderMessage), sender.getUUID());
            target.sendMessage(new StringTextComponent(targetMessage), target.getUUID());

            NexusBotMod.LOGGER.info("💌 MP: {} -> {}: {}", sender.getName().getString(), targetName, processedMessage);
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

    // ========== SET GLOBAL MODE ==========
    public void setGlobalMode(PlayerEntity player) {
        setChatMode(player.getStringUUID(), "global");
    }

    // ========== SET LOCAL MODE ==========
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