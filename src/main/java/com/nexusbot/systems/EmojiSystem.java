package com.nexusbot.systems;

import com.nexusbot.NexusBotMod;
import java.util.*;
import java.util.regex.Pattern;

public class EmojiSystem {

    // ========== EMOJIS 100% COMPATÍVEIS COM MINECRAFT ==========

    // ✅ EMOJIS DE EXPRESSÕES (Funcionam em todas as fontes)
    public static final Map<String, String> EXPRESSION_EMOJIS = new HashMap<String, String>() {{
        put(":)", "☺"); put(":D", "😀"); put(":(", "☹"); put(";)", "😉");
        put(":P", "😛"); put(":O", "😮"); put(":3", "😺"); put(":|", "😐");
        put(":/", "😕"); put(":'(", "😢"); put(":')", "😂"); put("B)", "😎");
        put("o.O", "😳"); put("O.o", "😲"); put("^_^", "😊"); put("-_-\"", "😑");
        put("<3", "❤"); put("</3", "💔"); put("(y)", "👍"); put("(n)", "👎");
    }};

    // ✅ EMOJIS DE OBJETOS E SÍMBOLOS
    public static final Map<String, String> OBJECT_EMOJIS = new HashMap<String, String>() {{
        put(":star:", "⭐"); put(":sun:", "☀"); put(":cloud:", "☁"); put(":umbrella:", "☂");
        put(":snowman:", "☃"); put(":comet:", "☄"); put(":phone:", "☎"); put(":flag:", "⚑");
        put(":anchor:", "⚓"); put(":sword:", "⚔"); put(":scales:", "⚖"); put(":gear:", "⚙");
        put(":pick:", "⛏"); put(":warning:", "⚠"); put(":radioactive:", "☢"); put(":biohazard:", "☣");
        put(":shamrock:", "☘"); put(":peace:", "☮"); put(":yin_yang:", "☯"); put(":wheel:", "☸");
        put(":spades:", "♠"); put(":hearts:", "♥"); put(":diamonds:", "♦"); put(":clubs:", "♣");
        put(":music:", "♪"); put(":recycle:", "♻"); put(":tm:", "™"); put(":copyright:", "©");
    }};

    // ✅ EMOJIS DE SETAS E FORMAS
    public static final Map<String, String> ARROW_EMOJIS = new HashMap<String, String>() {{
        put(":arrow_up:", "↑"); put(":arrow_down:", "↓"); put(":arrow_left:", "←");
        put(":arrow_right:", "→"); put(":left_right:", "↔"); put(":up_down:", "↕");
        put(":triangle_up:", "▲"); put(":triangle_down:", "▼"); put(":triangle_left:", "◀");
        put(":triangle_right:", "▶"); put(":circle:", "●"); put(":square:", "■");
        put(":diamond:", "◆"); put(":star5:", "★"); put(":star6:", "☆"); put(":bullet:", "•");
    }};

    // ✅ EMOJIS DE JOGO/MINECRAFT
    public static final Map<String, String> GAME_EMOJIS = new HashMap<String, String>() {{
        put(":creeper:", "💥"); put(":steve:", "👨"); put(":alex:", "👩"); put(":pickaxe:", "⛏");
        put(":sword:", "🗡"); put(":shield:", "🛡"); put(":bow:", "🏹"); put(":potion:", "🧪");
        put(":enchant:", "✨"); put(":xp:", "💎"); put(":diamond:", "💎"); put(":emerald:", "💚");
        put(":redstone:", "🔴"); put(":lapis:", "🔵"); put(":nether:", "🔥"); put(":end:", "🌌");
        put(":villager:", "🧔"); put(":zombie:", "🧟"); put(":skeleton:", "💀"); put(":spider:", "🕷");
        put(":ender:", "👁"); put(":ghast:", "👻"); put(":slime:", "🟢"); put(":wolf:", "🐺");
    }};

    // ✅ EMOJIS DE ATIVIDADES
    public static final Map<String, String> ACTIVITY_EMOJIS = new HashMap<String, String>() {{
        put(":mining:", "⛏"); put(":building:", "🏠"); put(":farming:", "🌾"); put(":fishing:", "🎣");
        put(":crafting:", "🛠"); put(":exploring:", "🧭"); put(":fighting:", "⚔"); put(":trading:", "🤝");
        put(":brewing:", "🧪"); put(":enchanting:", "✨"); put(":smelting:", "🔥"); put(":eating:", "🍎");
        put(":sleeping:", "😴"); put(":running:", "🏃"); put(":jumping:", "🦘"); put(":flying:", "✈");
    }};

    // ✅ EMOJIS COMPOSTOS (combinações)
    public static final Map<String, String> COMPOSITE_EMOJIS = new HashMap<String, String>() {{
        put(":fire_sword:", "🔥🗡"); put(":diamond_pick:", "💎⛏"); put(":enchanted_book:", "✨📖");
        put(":golden_apple:", "🍎⭐"); put(":ender_pearl:", "👁🔮"); put(":nether_portal:", "🔥🌀");
        put(":beacon:", "⭐🏔"); put(":redstone_torch:", "🔴🔥"); put(":cake:", "🎂🍰");
    }};

    // ========== SISTEMA DE CONVERSÃO ==========

    /**
     * Converte códigos de texto em emojis
     * Exemplo: ":)" → "☺"
     */
    public static String convertEmojiCodes(String message) {
        if (message == null || message.isEmpty()) return message;

        String result = message;

        // Converte em ordem de prioridade
        result = convertMap(EXPRESSION_EMOJIS, result);
        result = convertMap(OBJECT_EMOJIS, result);
        result = convertMap(ARROW_EMOJIS, result);
        result = convertMap(GAME_EMOJIS, result);
        result = convertMap(ACTIVITY_EMOJIS, result);
        result = convertMap(COMPOSITE_EMOJIS, result);

        return result;
    }

    private static String convertMap(Map<String, String> emojiMap, String message) {
        String result = message;
        for (Map.Entry<String, String> entry : emojiMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    // ========== SISTEMA DE VALIDAÇÃO ==========

    /**
     * Verifica se um emoji é compatível com Minecraft
     */
    public static boolean isEmojiCompatible(String emoji) {
        if (emoji == null || emoji.isEmpty()) return true;

        // Lista de emojis problemáticos conhecidos
        String[] problematicEmojis = {
                "👿", "😈", "🥵", "🤬", "🍆", "🍑", "💦", "🖕",
                "🏴", "🏴‍☠️", "🏳️‍🌈", "🏳️‍⚧️", "⚧", "❤️‍🔥", "❤️‍🩹"
        };

        for (String problematic : problematicEmojis) {
            if (emoji.contains(problematic)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Substitui emojis problemáticos por versões compatíveis
     */
    public static String replaceProblematicEmojis(String message) {
        if (message == null) return message;

        return message
                .replace("👿", "😤").replace("😈", "😏")
                .replace("🥵", "😅").replace("🤬", "😤")
                .replace("🍆", "🥒").replace("🍑", "🍐")
                .replace("💦", "💧").replace("🖕", "👎")
                .replace("🏴", "■").replace("🏴‍☠️", "■")
                .replace("🏳️‍🌈", "🌈").replace("🏳️‍⚧️", "⚥")
                .replace("⚧", "⚥").replace("❤️‍🔥", "❤️")
                .replace("❤️‍🩹", "❤️");
    }

    // ========== SISTEMA DE EMOJIS ALEATÓRIOS ==========

    /**
     * Pega um emoji aleatório de uma categoria
     */
    public static String getRandomEmoji(String category) {
        Map<String, String> emojiMap = null;

        switch (category.toLowerCase()) {
            case "expression": emojiMap = EXPRESSION_EMOJIS; break;
            case "object": emojiMap = OBJECT_EMOJIS; break;
            case "arrow": emojiMap = ARROW_EMOJIS; break;
            case "game": emojiMap = GAME_EMOJIS; break;
            case "activity": emojiMap = ACTIVITY_EMOJIS; break;
            default: emojiMap = EXPRESSION_EMOJIS;
        }

        if (emojiMap != null && !emojiMap.isEmpty()) {
            List<String> emojis = new ArrayList<>(emojiMap.values());
            return emojis.get(new Random().nextInt(emojis.size()));
        }

        return "✨"; // Emoji padrão
    }

    /**
     * Pega um emoji baseado em uma ação
     */
    public static String getEmojiForAction(String action) {
        if (action == null) return "✨";

        action = action.toLowerCase();

        if (action.contains("mine") || action.contains("miner")) return "⛏";
        if (action.contains("build") || action.contains("constru")) return "🏠";
        if (action.contains("farm") || action.contains("plant")) return "🌾";
        if (action.contains("fish")) return "🎣";
        if (action.contains("craft")) return "🛠";
        if (action.contains("fight") || action.contains("pvp")) return "⚔";
        if (action.contains("explore")) return "🧭";
        if (action.contains("trade")) return "🤝";
        if (action.contains("brew")) return "🧪";
        if (action.contains("enchant")) return "✨";
        if (action.contains("smelt")) return "🔥";
        if (action.contains("eat")) return "🍎";
        if (action.contains("sleep")) return "😴";
        if (action.contains("run")) return "🏃";
        if (action.contains("jump")) return "🦘";
        if (action.contains("fly")) return "✈";
        if (action.contains("death") || action.contains("die")) return "💀";
        if (action.contains("win") || action.contains("victory")) return "🏆";
        if (action.contains("lose") || action.contains("defeat")) return "😢";

        return "✨"; // Emoji padrão
    }

    // ========== SISTEMA DE MENSAGENS COM EMOJIS ==========

    /**
     * Adiciona emojis aleatórios a uma mensagem
     */
    public static String addRandomEmojis(String message, int maxEmojis) {
        if (message == null || maxEmojis <= 0) return message;

        Random random = new Random();
        String[] allEmojis = getAllCompatibleEmojis();

        if (allEmojis.length == 0) return message;

        StringBuilder result = new StringBuilder(message);
        int emojiCount = random.nextInt(maxEmojis) + 1;

        for (int i = 0; i < emojiCount; i++) {
            int position = random.nextInt(result.length() + 1);
            String emoji = allEmojis[random.nextInt(allEmojis.length)];
            result.insert(position, emoji + " ");
        }

        return result.toString().trim();
    }

    /**
     * Pega todos os emojis compatíveis
     */
    private static String[] getAllCompatibleEmojis() {
        Set<String> allEmojis = new HashSet<>();

        allEmojis.addAll(EXPRESSION_EMOJIS.values());
        allEmojis.addAll(OBJECT_EMOJIS.values());
        allEmojis.addAll(ARROW_EMOJIS.values());
        allEmojis.addAll(GAME_EMOJIS.values());
        allEmojis.addAll(ACTIVITY_EMOJIS.values());
        allEmojis.addAll(COMPOSITE_EMOJIS.values());

        return allEmojis.toArray(new String[0]);
    }

    // ========== VALIDAÇÃO DE STRING ==========

    /**
     * Verifica se uma string contém apenas caracteres compatíveis
     */
    public static boolean isStringCompatible(String text) {
        if (text == null) return true;

        // Padrão de caracteres problemáticos
        Pattern problematicPattern = Pattern.compile(
                "[\\uD83C\\uDFF4\\uDB40\\uDC67\\uDB40\\uDC62\\uDB40\\uDC77\\uDB40\\uDC6C\\uDB40\\uDC73\\uDB40\\uDC7F]" + // Bandeiras
                        "|[\\uD83D\\uDC69\\u200D\\u2764\\uFE0F\\u200D\\uD83D\\uDC68]" + // Casais
                        "|[\\uD83D\\uDC68\\u200D\\uD83D\\uDC68\\u200D\\uD83D\\uDC66\\u200D\\uD83D\\uDC66]" // Famílias
        );

        return !problematicPattern.matcher(text).find();
    }

    /**
     * Limpa uma string de caracteres problemáticos
     */
    public static String cleanString(String text) {
        if (text == null) return text;

        // Remove caracteres ZWJ (Zero Width Joiner) que causam problemas
        text = text.replaceAll("[\\u200D\\u200B\\u200C\\u200E\\u200F\\uFE0F]", "");

        // Remove sequências problemáticas específicas
        text = text.replaceAll("[\\uD83C\\uDFF4\\uDB40\\uDC67\\uDB40\\uDC62\\uDB40\\uDC77\\uDB40\\uDC6C\\uDB40\\uDC73\\uDB40\\uDC7F]", "■");
        text = text.replaceAll("[\\uD83D\\uDC69\\u200D\\u2764\\uFE0F\\u200D\\uD83D\\uDC68]", "❤️");
        text = text.replaceAll("[\\uD83D\\uDC68\\u200D\\uD83D\\uDC68\\u200D\\uD83D\\uDC66\\u200D\\uD83D\\uDC66]", "👨👩👧👦");

        return text;
    }
}