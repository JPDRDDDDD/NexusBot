package com.nexusbot.systems;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.MessageChannel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.server.ServerWorld;
import com.nexusbot.NexusBotMod;
import reactor.core.publisher.Mono;

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

    private final ScheduledExecutorService botScheduler = Executors.newScheduledThreadPool(1);
    private EventSystem eventSystem;

    // ========== SISTEMA DE IA SIMPLIFICADO ==========
    private final Map<String, Long> lastBotResponse = new HashMap<>();
    private final Random random = new Random();

    public ChatSystem() {
        this.eventSystem = new EventSystem();
        NexusBotMod.LOGGER.info("Sistema de Chat iniciado");
    }

    // ========== GETTER PARA EVENT SYSTEM ==========
    public EventSystem getEventSystem() {
        return this.eventSystem;
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

        // Pode expandir para um sistema mais complexo se necessário
    }

    // ========== SISTEMA DE MENSAGENS DO BOT PARA EVENTOS ==========

    public void onPlayerJoin(PlayerEntity player) {
        String playerName = player.getName().getString();
        String welcomeMessage = playerName + " entrou no servidor! Bem-vindo(a)!";
        sendBotMessage(welcomeMessage);
    }

    public void onPlayerLeave(PlayerEntity player) {
        String playerName = player.getName().getString();
        String leaveMessage = playerName + " saiu do servidor. Ate mais!";
        sendBotMessage(leaveMessage);
    }

    public void onPlayerAdvancement(PlayerEntity player, String advancementName) {
        String playerName = player.getName().getString();

        // ✅ LOG PARA FACILITAR A DESCOBERTA DOS IDs
        eventSystem.logAdvancementDetection(player, advancementName);

        // Primeiro verifica se tem evento customizado
        String customMessage = eventSystem.getCustomEventMessage(advancementName, playerName);
        if (customMessage != null) {
            sendBotMessage(customMessage);
            return;
        }

        // Se não tiver, usa as mensagens padrão
        String message = getAdvancementMessage(playerName, advancementName);
        if (message != null) {
            sendBotMessage(message);
        }
    }

    private String getAdvancementMessage(String playerName, String advancementName) {
        // Conquistas do Minecraft Vanilla
        switch (advancementName.toLowerCase()) {
            // Mineração
            case "minecraft:story/mine_stone":
            case "minecraft.story.mine_stone":
                return playerName + " comecou sua jornada minerando! Que venham os recursos!";

            case "minecraft:story/iron_tools":
            case "minecraft.story.iron_tools":
                return playerName + " agora tem ferramentas de ferro! Hora de melhorar!";

            case "minecraft:story/mine_diamond":
            case "minecraft.story.mine_diamond":
                String randomPlayer = getRandomOnlinePlayer();
                return playerName + " encontrou DIAMANTES! " + randomPlayer + ", voce tambem quer uns?";

            case "minecraft:story/enchant_item":
            case "minecraft.story.enchant_item":
                return playerName + " encantou um item! Magia no ar!";

            // Nether
            case "minecraft:story/enter_the_nether":
            case "minecraft.story.enter_the_nether":
                return playerName + " entrou no Nether! Cuidado com os perigos!";

            case "minecraft:story/find_fortress":
            case "minecraft.story.find_fortress":
                return playerName + " encontrou uma fortaleza do Nether! Hora da aventura!";

            // Fim
            case "minecraft:story/enter_the_end":
            case "minecraft.story.enter_the_end":
                return playerName + " chegou ao Fim! Preparem-se para o dragao, galera!";

            // Agricultura
            case "minecraft:husbandry/plant_seed":
            case "minecraft.husbandry.plant_seed":
                return playerName + " comecou uma plantacao! Futuro fazendeiro!";

            case "minecraft:husbandry/breed_an_animal":
            case "minecraft.husbandry.breed_an_animal":
                return playerName + " esta criando animais! Que fofura!";

            // Combate
            case "minecraft:adventure/kill_a_mob":
            case "minecraft.adventure.kill_a_mob":
                return playerName + " derrotou seu primeiro mob! Guerreiro em formacao!";

            case "minecraft:adventure/kill_all_mobs":
            case "minecraft.adventure.kill_all_mobs":
                return playerName + " completou o bestiario! Mestre dos monstros!";

            // Conquistas especiais
            case "minecraft:end/kill_dragon":
            case "minecraft.end.kill_dragon":
                String dragonPlayer = getRandomOnlinePlayer();
                return playerName + " MATOU O DRAGAO DO FIM! " + dragonPlayer + ", vamos comemorar!";

            case "minecraft:end/elytra":
            case "minecraft.end.elytra":
                return playerName + " conseguiu uma Elytra! Hora de voar!";

            case "minecraft:nether/get_wither_skull":
            case "minecraft.nether.get_wither_skull":
                return playerName + " conseguiu uma cabeca de Wither! Cuidado galera!";

            case "minecraft:nether/summon_wither":
            case "minecraft.nether.summon_wither":
                return playerName + " invocou o Wither! Todos em alerta!";

            // Conquistas de mods (exemplos genericos)
            default:
                if (advancementName.contains("diamond") || advancementName.contains("diamante")) {
                    String diamondPlayer = getRandomOnlinePlayer();
                    return playerName + " conquistou algo com DIAMANTES! " + diamondPlayer + ", quer compartilhar?";
                }
                else if (advancementName.contains("nether") || advancementName.contains("inferno")) {
                    return playerName + " explorou o Nether! Corajoso!";
                }
                else if (advancementName.contains("end") || advancementName.contains("fim")) {
                    return playerName + " desbravou o Fim! Aventureiro!";
                }
                else if (advancementName.contains("boss") || advancementName.contains("chefe")) {
                    return playerName + " derrotou um boss! Forca guerreiro!";
                }
                else if (advancementName.contains("magic") || advancementName.contains("magia") || advancementName.contains("encant")) {
                    return playerName + " dominou a magia! Poderoso!";
                }
                else if (advancementName.contains("build") || advancementName.contains("constru")) {
                    return playerName + " mostrou suas habilidades de construcao! Arquiteto!";
                }
                else if (advancementName.contains("explore") || advancementName.contains("explor")) {
                    return playerName + " e um grande explorador! Novas terras descobertas!";
                }
                else if (advancementName.contains("craft") || advancementName.contains("criar")) {
                    return playerName + " criou algo incrivel! Artesao talentoso!";
                }
                else {
                    // Mensagem generica para outras conquistas
                    return playerName + " conquistou: " + formatAdvancementName(advancementName) + "! Parabens!";
                }
        }
    }

    private String formatAdvancementName(String advancementName) {
        // Formata o nome da conquista para ficar mais legivel
        String formatted = advancementName
                .replace("minecraft:", "")
                .replace("minecraft.", "")
                .replace("story/", "")
                .replace("story.", "")
                .replace("husbandry/", "")
                .replace("husbandry.", "")
                .replace("adventure/", "")
                .replace("adventure.", "")
                .replace("nether/", "")
                .replace("nether.", "")
                .replace("end/", "")
                .replace("end.", "")
                .replace("_", " ")
                .replace("/", " ");

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
        eventSystem.addCustomEvent(advancementId, message);
    }

    public void removeCustomEvent(String advancementId) {
        eventSystem.removeCustomEvent(advancementId);
    }

    public Map<String, String> getCustomEvents() {
        return eventSystem.getCustomEvents();
    }

    public List<String> getAvailableAdvancements() {
        return eventSystem.getAvailableAdvancements();
    }

    // ========== SISTEMA DE CORES ==========
    public void showColorCodes(PlayerEntity player) {
        player.sendMessage(new StringTextComponent("§6§l📚 CODIGOS DE CORES DISPONIVEIS:"), player.getUUID());
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

        player.sendMessage(new StringTextComponent("§k&k §kTexto Aleatorio"), player.getUUID());
        player.sendMessage(new StringTextComponent("§l&l §lNegrito"), player.getUUID());
        player.sendMessage(new StringTextComponent("§m&m §mTachado"), player.getUUID());
        player.sendMessage(new StringTextComponent("§n&n §nSublinhado"), player.getUUID());
        player.sendMessage(new StringTextComponent("§o&o §oItalico"), player.getUUID());
        player.sendMessage(new StringTextComponent("§r&r §rReset"), player.getUUID());
        player.sendMessage(new StringTextComponent(""), player.getUUID());

        player.sendMessage(new StringTextComponent("§7Exemplo: &cParabens &4Voce conseguiu &2Seu primeiro &bDraconic &3Core &nContinue assim&r!"), player.getUUID());
        player.sendMessage(new StringTextComponent("§7Resultado: §cParabens §4Voce conseguiu §2Seu primeiro §bDraconic §3Core §nContinue assim§r!"), player.getUUID());
    }

    // ========== ENVIO DE MENSAGENS DO BOT ==========
    public void sendBotMessage(String message) {
        if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
            String formattedMessage = "§8[§6NexusBot§8] §e" + message;
            StringTextComponent textComponent = new StringTextComponent(formattedMessage);

            textComponent.withStyle(style -> style.withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new StringTextComponent("§6NexusBot\n§7Sistema de notificacoes automaticas")
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
            player.sendMessage(new StringTextComponent("§c§l⚠ §cVoce esta §lMUTADO§c e nao pode falar no chat!"), player.getUUID());
            return;
        }

        if (!hasBypass(playerName)) {
            if (detectBadWords(message)) {
                player.sendMessage(new StringTextComponent("§c§l🚫 §cSua mensagem contem palavras proibidas!"), player.getUUID());
                return;
            }

            if (detectSpam(playerUUID, message)) {
                player.sendMessage(new StringTextComponent("§c§l⚠ §cNao faca §lSPAM§c no chat!"), player.getUUID());
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
        if(NexusBotMod.botdc!=null && !NexusBotMod.canalID.isEmpty()){
            NexusBotMod.botdc.getChannelById(Snowflake.of(NexusBotMod.canalID))
                    .ofType(MessageChannel.class)
                    .flatMap(channel -> channel.createMessage("**"+player.getName().getString()+"**: "+message))
                    .onErrorResume(err -> {
                        NexusBotMod.LOGGER.error("[NexusBot] erro ao enviar a mensagem");
                        return Mono.empty();
                    }).subscribe();
        }
    }

    // ========== CHAT STAFF ==========
    public void sendStaffMessage(PlayerEntity player, String message) {
        String formattedMessage = "§8[§4Staff§8] §c" + player.getName().getString() + " §8» §f" + message;
        StringTextComponent textComponent = new StringTextComponent(formattedMessage);

        textComponent.withStyle(style -> style.withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new StringTextComponent("§4Chat da Staff\n§7Apenas jogadores com permissao de §cOP§7 veem esta mensagem")
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

    // ========== MENSAGEM PRIVADA ==========
    public void sendPrivateMessage(PlayerEntity sender, String targetName, String message) {
        if (sender.getServer() == null) return;

        ServerPlayerEntity target = sender.getServer().getPlayerList().getPlayerByName(targetName);
        if (target != null) {
            String senderMessage = "§8[§d" + targetName + "§8] §7Voce §8» §f" + message;
            String targetMessage = "§8[§d" + sender.getName().getString() + "§8] §7" + sender.getName().getString() + " §8» §f" + message;

            sender.sendMessage(new StringTextComponent(senderMessage), sender.getUUID());
            target.sendMessage(new StringTextComponent(targetMessage), target.getUUID());

            NexusBotMod.LOGGER.info("MP: {} -> {}: {}", sender.getName().getString(), targetName, message);
        } else {
            sender.sendMessage(new StringTextComponent("§c§l❌ §cJogador '§f" + targetName + "§c' nao encontrado!"), sender.getUUID());
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

    // ========== SISTEMA DE MODO DE CHAT ==========
    public void setGlobalMode(PlayerEntity player) {
        setChatMode(player.getStringUUID(), "global");
    }

    public void setLocalMode(PlayerEntity player) {
        setChatMode(player.getStringUUID(), "local");
    }

    public void setChatMode(String playerUUID, String mode) {
        chatModes.put(playerUUID, mode);
        NexusBotMod.LOGGER.info("Modo de chat alterado: {} -> {}", playerUUID, mode);
    }

    public String getChatMode(String playerUUID) {
        return chatModes.getOrDefault(playerUUID, "local");
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

    private String getRandomOnlinePlayer() {
        List<String> players = getOnlinePlayers();
        if (players.isEmpty()) return "Galera";
        return players.get(new Random().nextInt(players.size()));
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