package com.nexusbot.systems;

import com.nexusbot.NexusBotMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventSystem {
    private final String EVENTS_FILE = "nexusbot_events.txt";
    private Map<String, String> customEvents = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private Set<String> detectedAdvancements = new ConcurrentHashMap<String, Boolean>().keySet(true);

    // Mapeamento de mods para IDs de conquistas
    private Map<String, List<String>> modAdvancements = new HashMap<>();

    public EventSystem() {
        loadEventsFromFile();
        initializeModAdvancements();
        NexusBotMod.LOGGER.info("Sistema de Eventos carregado: {} eventos", customEvents.size());
        NexusBotMod.LOGGER.info("Mods detectados para autocomplete: {}", modAdvancements.size());
    }

    // ========== SISTEMA DE DETECÇÃO AUTOMÁTICA DE MODS ==========
    private void initializeModAdvancements() {
        modAdvancements.clear();

        // ✅ CONQUISTAS VANILLA (sempre disponíveis)
        modAdvancements.put("minecraft", Arrays.asList(
                "minecraft:story/root",
                "minecraft:story/mine_stone",
                "minecraft:story/upgrade_tools",
                "minecraft:story/smelt_iron",
                "minecraft:story/obtain_armor",
                "minecraft:story/lava_bucket",
                "minecraft:story/iron_tools",
                "minecraft:story/deflect_arrow",
                "minecraft:story/form_obsidian",
                "minecraft:story/mine_diamond",
                "minecraft:story/enter_the_nether",
                "minecraft:story/shiny_gear",
                "minecraft:story/enchant_item",
                "minecraft:story/cure_zombie_villager",
                "minecraft:story/follow_ender_eye",
                "minecraft:story/enter_the_end",
                "minecraft:nether/root",
                "minecraft:nether/return_to_sender",
                "minecraft:nether/find_bastion",
                "minecraft:nether/find_fortress",
                "minecraft:nether/get_wither_skull",
                "minecraft:nether/obtain_blaze_rod",
                "minecraft:nether/obtain_crying_obsidian",
                "minecraft:nether/distract_piglin",
                "minecraft:nether/ride_strider",
                "minecraft:nether/uneasy_alliance",
                "minecraft:nether/loot_bastion",
                "minecraft:nether/use_lodestone",
                "minecraft:nether/netherite_armor",
                "minecraft:nether/get_debris",
                "minecraft:nether/obtain_ancient_debris",
                "minecraft:nether/fast_travel",
                "minecraft:nether/find_fortress",
                "minecraft:nether/obtain_crying_obsidian",
                "minecraft:nether/charge_respawn_anchor",
                "minecraft:end/root",
                "minecraft:end/kill_dragon",
                "minecraft:end/enter_end_gateway",
                "minecraft:end/respawn_dragon",
                "minecraft:end/dragon_egg",
                "minecraft:end/elytra",
                "minecraft:end/find_end_city",
                "minecraft:adventure/root",
                "minecraft:adventure/voluntary_exile",
                "minecraft:adventure/kill_a_mob",
                "minecraft:adventure/trade",
                "minecraft:adventure/honey_block_slide",
                "minecraft:adventure/ol_betsy",
                "minecraft:adventure/sleep_in_bed",
                "minecraft:adventure/shoot_arrow",
                "minecraft:adventure/kill_all_mobs",
                "minecraft:adventure/totem_of_undying",
                "minecraft:adventure/summon_iron_golem",
                "minecraft:adventure/two_birds_one_arrow",
                "minecraft:adventure/whos_the_pillager_now",
                "minecraft:adventure/arbalistic",
                "minecraft:adventure/adventuring_time",
                "minecraft:adventure/hero_of_the_village",
                "minecraft:adventure/throw_trident",
                "minecraft:adventure/very_very_frightening",
                "minecraft:adventure/sniper_duel",
                "minecraft:husbandry/root",
                "minecraft:husbandry/safely_harvest_honey",
                "minecraft:husbandry/breed_an_animal",
                "minecraft:husbandry/tame_an_animal",
                "minecraft:husbandry/fishy_business",
                "minecraft:husbandry/silk_touch_nest",
                "minecraft:husbandry/plant_seed",
                "minecraft:husbandry/kill_axolotl_target",
                "minecraft:husbandry/complete_catalogue",
                "minecraft:husbandry/tactical_fishing",
                "minecraft:husbandry/balanced_diet",
                "minecraft:husbandry/obtain_netherite_hoe"
        ));

        // ✅ DRACONIC EVOLUTION (presente no seu servidor)
        if (isModLoaded("draconicevolution")) {
            modAdvancements.put("draconicevolution", Arrays.asList(
                    "draconicevolution:wyvern_core",
                    "draconicevolution:awakened_core",
                    "draconicevolution:draconic_core",
                    "draconicevolution:chaotic_core",
                    "draconicevolution:wyvern_tools",
                    "draconicevolution:draconic_tools",
                    "draconicevolution:chaotic_tools",
                    "draconicevolution:wyvern_armor",
                    "draconicevolution:draconic_armor",
                    "draconicevolution:chaotic_armor",
                    "draconicevolution:reactor",
                    "draconicevolution:energy_core"
            ));
        }

        // ✅ MEKANISM (presente no seu servidor)
        if (isModLoaded("mekanism")) {
            modAdvancements.put("mekanism", Arrays.asList(
                    "mekanism:root",
                    "mekanism:metallurgic_infuser",
                    "mekanism:purification_chamber",
                    "mekanism:crusher",
                    "mekanism:enrichment_chamber",
                    "mekanism:osmium_compressor",
                    "mekanism:combiner",
                    "mekanism:digital_miner",
                    "mekanism:atomic_disassembler",
                    "mekanism:jetpack",
                    "mekanism:mekasuit",
                    "mekanism:qio_drive"
            ));
        }

        // ✅ TINKERS CONSTRUCT (presente no seu servidor)
        if (isModLoaded("tconstruct")) {
            modAdvancements.put("tconstruct", Arrays.asList(
                    "tconstruct:story/root",
                    "tconstruct:story/melting",
                    "tconstruct:story/casts",
                    "tconstruct:story/tinkers_anvil",
                    "tconstruct:world/slime_island",
                    "tconstruct:tools/station",
                    "tconstruct:tools/pickaxe",
                    "tconstruct:tools/sledge_hammer",
                    "tconstruct:tools/broad_axe",
                    "tconstruct:tools/scythe",
                    "tconstruct:tools/cleaver",
                    "tconstruct:tools/kama"
            ));
        }

        // ✅ BOTANIA (presente no seu servidor)
        if (isModLoaded("botania")) {
            modAdvancements.put("botania", Arrays.asList(
                    "botania:main/root",
                    "botania:main/flower_pickup",
                    "botania:main/lexicon_use",
                    "botania:main/mana_pool_craft",
                    "botania:main/rune_pickup",
                    "botania:main/terrasteel_pickup",
                    "botania:main/gaia_guardian_kill",
                    "botania:challenge/rank_ss_pick"
            ));
        }

        // ✅ ARS NOUVEAU (presente no seu servidor)
        if (isModLoaded("ars_nouveau")) {
            modAdvancements.put("ars_nouveau", Arrays.asList(
                    "ars_nouveau:root",
                    "ars_nouveau:novice_spellbook",
                    "ars_nouveau:apprentice_spellbook",
                    "ars_nouveau:archmage_spellbook",
                    "ars_nouveau:enchanting_apparatus",
                    "ars_nouveau:source_jar",
                    "ars_nouveau:spell_turret",
                    "ars_nouveau:wixie_charm"
            ));
        }

        // ✅ APOTHEOSIS (presente no seu servidor)
        if (isModLoaded("apotheosis")) {
            modAdvancements.put("apotheosis", Arrays.asList(
                    "apotheosis:root",
                    "apotheosis:affix_gear",
                    "apotheosis:mythic_gear",
                    "apotheosis:spawner",
                    "apotheosis:boss"
            ));
        }

        // ✅ TWILIGHT FOREST (presente no seu servidor)
        if (isModLoaded("twilightforest")) {
            modAdvancements.put("twilightforest", Arrays.asList(
                    "twilightforest:root",
                    "twilightforest:progress_lich",
                    "twilightforest:progress_ur_ghast",
                    "twilightforest:progress_yeti",
                    "twilightforest:progress_naga",
                    "twilightforest:progress_knights",
                    "twilightforest:progress_troll",
                    "twilightforest:progress_glacier",
                    "twilightforest:progress_final_castle"
            ));
        }

        // ✅ BLOOD MAGIC (presente no seu servidor)
        if (isModLoaded("bloodmagic")) {
            modAdvancements.put("bloodmagic", Arrays.asList(
                    "bloodmagic:root",
                    "bloodmagic:altar",
                    "bloodmagic:alchemy_table",
                    "bloodmagic:soul_forge",
                    "bloodmagic:ritual_stone",
                    "bloodmagic:ritual_master"
            ));
        }

        // ✅ CREATE (presente no seu servidor)
        if (isModLoaded("create")) {
            modAdvancements.put("create", Arrays.asList(
                    "create:root",
                    "create:water_wheel",
                    "create:windmill",
                    "create:encased_fan",
                    "create:mechanical_press",
                    "create:mechanical_mixer",
                    "create:deployer",
                    "create:mechanical_drill",
                    "create:mechanical_saw",
                    "create:mechanical_harvester",
                    "create:mechanical_plough",
                    "create:contraption"
            ));
        }

        // ✅ CYCLIC (presente no seu servidor)
        if (isModLoaded("cyclic")) {
            modAdvancements.put("cyclic", Arrays.asList(
                    "cyclic:root",
                    "cyclic:apple_ender",
                    "cyclic:apple_chorus",
                    "cyclic:apple_prismarine",
                    "cyclic:apple_honey",
                    "cyclic:apple_lapis",
                    "cyclic:apple_emerald"
            ));
        }

        // ✅ FORBIDDEN ARCANUS (presente no seu servidor)
        if (isModLoaded("forbidden_arcanus")) {
            modAdvancements.put("forbidden_arcanus", Arrays.asList(
                    "forbidden_arcanus:root",
                    "forbidden_arcanus:obtain_rune",
                    "forbidden_arcanus:obtain_arcane_crystal",
                    "forbidden_arcanus:obtain_dark_nether_star",
                    "forbidden_arcanus:obtain_eternal_stella"
            ));
        }

        // ✅ VAMPIRISM (presente no seu servidor)
        if (isModLoaded("vampirism")) {
            modAdvancements.put("vampirism", Arrays.asList(
                    "vampirism:root",
                    "vampirism:become_vampire",
                    "vampirism:become_hunter",
                    "vampirism:vampire_level_5",
                    "vampirism:hunter_level_5",
                    "vampirism:kill_vampire_village"
            ));
        }

        // ✅ RATS (presente no seu servidor)
        if (isModLoaded("rats")) {
            modAdvancements.put("rats", Arrays.asList(
                    "rats:root",
                    "rats:rat_taming",
                    "rats:rat_upgrade_chef",
                    "rats:rat_upgrade_basic",
                    "rats:rat_upgrade_combined",
                    "rats:rat_upgrade_aristocrat"
            ));
        }

        // ✅ ALLTHEMODIUM (presente no seu servidor)
        if (isModLoaded("allthemodium")) {
            modAdvancements.put("allthemodium", Arrays.asList(
                    "allthemodium:root",
                    "allthemodium:allthemodium_ingot",
                    "allthemodium:vibranium_ingot",
                    "allthemodium:unobtainium_ingot",
                    "allthemodium:allthemodium_armor",
                    "allthemodium:allthemodium_tools"
            ));
        }

        NexusBotMod.LOGGER.info("✅ Sistema de autocomplete carregado com sucesso!");
    }

    // ========== VERIFICA SE MOD ESTÁ CARREGADO ==========
    private boolean isModLoaded(String modId) {
        try {
            return ModList.get().isLoaded(modId);
        } catch (Exception e) {
            NexusBotMod.LOGGER.warn("❌ Erro ao verificar mod {}: {}", modId, e.getMessage());
            return false;
        }
    }

    // ========== SISTEMA DE AUTCOMPLETE INTELIGENTE ==========
    public List<String> getAdvancementSuggestions(String partialId) {
        List<String> suggestions = new ArrayList<>();

        if (partialId == null || partialId.trim().isEmpty()) {
            // Retorna todas as conquistas disponíveis
            for (List<String> advancements : modAdvancements.values()) {
                suggestions.addAll(advancements);
            }
            // Adiciona conquistas detectadas
            suggestions.addAll(detectedAdvancements);
        } else {
            String searchTerm = partialId.toLowerCase();

            // Busca em todas as conquistas
            for (List<String> advancements : modAdvancements.values()) {
                for (String advancement : advancements) {
                    if (advancement.toLowerCase().contains(searchTerm)) {
                        suggestions.add(advancement);
                    }
                }
            }

            // Busca nas conquistas detectadas
            for (String advancement : detectedAdvancements) {
                if (advancement.toLowerCase().contains(searchTerm)) {
                    suggestions.add(advancement);
                }
            }
        }

        // Remove duplicatas e ordena
        Set<String> uniqueSuggestions = new TreeSet<>(suggestions);
        return new ArrayList<>(uniqueSuggestions);
    }

    // ========== GET MODS DISPONÍVEIS PARA AUTCOMPLETE ==========
    public Map<String, List<String>> getAvailableModAdvancements() {
        return new HashMap<>(modAdvancements);
    }

    // ========== GET SUGESTÕES POR MOD ==========
    public List<String> getAdvancementsByMod(String modId) {
        return modAdvancements.getOrDefault(modId, new ArrayList<>());
    }

    // ========== GET MODS INSTALADOS ==========
    public List<String> getInstalledMods() {
        return new ArrayList<>(modAdvancements.keySet());
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