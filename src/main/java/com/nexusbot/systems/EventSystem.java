package com.nexusbot.systems;

import com.nexusbot.NexusBotMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventSystem {
    private final String EVENTS_FILE = "nexusbot_events.txt";
    private Map<String, String> customEvents = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private Set<String> detectedAdvancements = new ConcurrentHashMap<String, Boolean>().keySet(true);

    public EventSystem() {
        loadEventsFromFile();
        NexusBotMod.LOGGER.info("Sistema de Eventos carregado: {} eventos", customEvents.size());
    }

    // ========== SISTEMA DE CORES ==========
    public static String translateColors(String message) {
        if (message == null) return null;

        return message
                .replace("&0", "§0") // Preto
                .replace("&1", "§1") // Azul Escuro
                .replace("&2", "§2") // Verde Escuro
                .replace("&3", "§3") // Azul Claro
                .replace("&4", "§4") // Vermelho
                .replace("&5", "§5") // Roxo
                .replace("&6", "§6") // Laranja
                .replace("&7", "§7") // Cinza
                .replace("&8", "§8") // Cinza Escuro
                .replace("&9", "§9") // Azul
                .replace("&a", "§a") // Verde
                .replace("&b", "§b") // Azul Claro
                .replace("&c", "§c") // Vermelho Claro
                .replace("&d", "§d") // Rosa
                .replace("&e", "§e") // Amarelo
                .replace("&f", "§f") // Branco
                .replace("&k", "§k") // Texto Aleatório
                .replace("&l", "§l") // Negrito
                .replace("&m", "§m") // Tachado
                .replace("&n", "§n") // Sublinhado
                .replace("&o", "§o") // Itálico
                .replace("&r", "§r"); // Reset
    }

    // ========== SISTEMA DE EVENTOS ==========
    public void addCustomEvent(String advancementId, String message) {
        customEvents.put(advancementId.toLowerCase(), message);
        saveEventsToFile();
        NexusBotMod.LOGGER.info("Evento adicionado: {} -> {}", advancementId, message);
    }

    public void removeCustomEvent(String advancementId) {
        String removed = customEvents.remove(advancementId.toLowerCase());
        if (removed != null) {
            saveEventsToFile();
            NexusBotMod.LOGGER.info("Evento removido: {}", advancementId);
        }
    }

    public String getCustomEventMessage(String advancementId, String playerName) {
        String message = customEvents.get(advancementId.toLowerCase());
        if (message != null) {
            // Substitui placeholders e aplica cores
            String processed = message
                    .replace("{player}", playerName)
                    .replace("{random}", getRandomOnlinePlayer());
            return translateColors(processed);
        }
        return null;
    }

    public boolean hasCustomEvent(String advancementId) {
        return customEvents.containsKey(advancementId.toLowerCase());
    }

    public Map<String, String> getCustomEvents() {
        return new HashMap<>(customEvents);
    }

    // ========== SISTEMA DE DETECÇÃO AUTOMÁTICA ==========
    public void logAdvancementDetection(PlayerEntity player, String advancementName) {
        // Adiciona à lista de conquistas detectadas
        detectedAdvancements.add(advancementName);

        NexusBotMod.LOGGER.info("🎯 CONQUISTA DETECTADA: {} -> {}", player.getName().getString(), advancementName);

        // Notifica admins online
        notifyAdminsAboutAdvancement(player.getName().getString(), advancementName);
    }

    private void notifyAdminsAboutAdvancement(String playerName, String advancementName) {
        if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayerEntity staff : net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (staff.hasPermissions(2)) {
                    staff.sendMessage(new StringTextComponent(
                            "§6🎯 §eConquista Detectada: §b" + playerName +
                                    " §7-> §a" + advancementName +
                                    " §8(Use: /bot-evento add " + advancementName + " \"sua mensagem\")"
                    ), staff.getUUID());
                }
            }
        }
    }

    public List<String> getDetectedAdvancements() {
        return new ArrayList<>(detectedAdvancements);
    }

    public List<String> getAvailableAdvancements() {
        List<String> available = new ArrayList<>();

        // Conquistas do Minecraft Vanilla (mais comuns)
        available.add("minecraft:story/mine_stone");
        available.add("minecraft:story/mine_diamond");
        available.add("minecraft:story/enter_the_nether");
        available.add("minecraft:story/enter_the_end");
        available.add("minecraft:end/kill_dragon");
        available.add("minecraft:end/elytra");
        available.add("minecraft:nether/summon_wither");
        available.add("minecraft:adventure/kill_all_mobs");
        available.add("minecraft:husbandry/breed_an_animal");

        // Conquistas de mods populares
        available.add("draconicevolution:wyvern_core");
        available.add("draconicevolution:awakened_core");
        available.add("draconicevolution:draconic_core");
        available.add("appliedenergistics2:charger");
        available.add("appliedenergistics2:controller");
        available.add("tconstruct:tools/station");
        available.add("tconstruct:tools/pickaxe");
        available.add("botania:flower_pickup");
        available.add("botania:rank_ss_pick");

        // Adiciona as conquistas que foram detectadas automaticamente
        available.addAll(detectedAdvancements);

        // Remove duplicatas e ordena
        Set<String> uniqueAdvancements = new TreeSet<>(available);
        return new ArrayList<>(uniqueAdvancements);
    }

    // ========== SISTEMA DE ARQUIVO ==========
    private void loadEventsFromFile() {
        try {
            File file = new File(EVENTS_FILE);
            if (!file.exists()) {
                createDefaultEvents();
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.startsWith("#")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        customEvents.put(parts[0].toLowerCase().trim(), parts[1].trim());
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            NexusBotMod.LOGGER.error("Erro ao carregar eventos: {}", e.toString());
        }
    }

    private void saveEventsToFile() {
        try {
            FileWriter writer = new FileWriter(EVENTS_FILE);
            writer.write("# NexusBot Eventos Customizados\n");
            writer.write("# Formato: advancement_id=mensagem com cores\n");
            writer.write("# Cores: &0-&9, &a-&f, &k-&r\n");
            writer.write("# Placeholders: {player} = nome do jogador, {random} = jogador aleatorio\n\n");

            for (Map.Entry<String, String> entry : customEvents.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
            }
            writer.close();
        } catch (IOException e) {
            NexusBotMod.LOGGER.error("Erro ao salvar eventos: {}", e.toString());
        }
    }

    private void createDefaultEvents() {
        // Eventos padrão do Minecraft
        addCustomEvent("minecraft:story/mine_diamond",
                "&6{player} &eencontrou &bDIAMANTES&e! &a{random}&e, quer compartilhar a sorte?");

        addCustomEvent("minecraft:end/kill_dragon",
                "&4{player} &cMATOU O DRAGAO DO FIM&4! &6{random}&4, vamos comemorar essa vitoria!");

        addCustomEvent("minecraft:nether/summon_wither",
                "&8{player} &7invocou o &8WITHER&7! &cCuidado galera&7!");

        addCustomEvent("minecraft:end/elytra",
                "&b{player} &9conseguiu uma &3Elytra&9! &eHora de voar pelos ceus!");

        saveEventsToFile();
    }

    private String getRandomOnlinePlayer() {
        List<String> players = getOnlinePlayers();
        if (players.isEmpty()) return "Galera";
        return players.get(random.nextInt(players.size()));
    }

    private List<String> getOnlinePlayers() {
        List<String> onlinePlayers = new ArrayList<>();
        if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
            for (net.minecraft.entity.player.ServerPlayerEntity player :
                    net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                onlinePlayers.add(player.getName().getString());
            }
        }
        return onlinePlayers;
    }
}