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

    public EventSystem() {
        loadEventsFromFile();
        NexusBotMod.LOGGER.info("Sistema de Eventos carregado: {} eventos", customEvents.size());
    }

    // ========== SISTEMA DE MENSAGENS PERSONALIZADAS ==========
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

    // ========== SISTEMA DE EVENTOS CUSTOMIZADOS ==========
    public void addCustomEvent(String advancementId, String message) {
        customEvents.put(advancementId.toLowerCase(), message);
        saveEventsToFile();
    }

    public void removeCustomEvent(String advancementId) {
        customEvents.remove(advancementId.toLowerCase());
        saveEventsToFile();
    }

    public Map<String, String> getCustomEvents() {
        return new HashMap<>(customEvents);
    }

    // ========== MÉTODOS AUXILIARES ==========
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

    // ========== SISTEMA DE ARQUIVO ==========
    private void loadEventsFromFile() {
        try {
            File file = new File(EVENTS_FILE);
            if (!file.exists()) {
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
            writer.write("# Cores: use & para cores (ex: &aVerde &cVermelho)\n");
            writer.write("# Placeholders: {player} = nome do jogador\n\n");

            for (Map.Entry<String, String> entry : customEvents.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
            }
            writer.close();
        } catch (IOException e) {
            NexusBotMod.LOGGER.error("Erro ao salvar eventos: {}", e.toString());
        }
    }
}