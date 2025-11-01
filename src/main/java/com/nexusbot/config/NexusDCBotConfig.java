package com.nexusbot.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class NexusDCBotConfig {
    // Campos para o token e o ID do canal
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final NexusDCBotConfig COMMON;

    public final ForgeConfigSpec.ConfigValue<String> botToken;
    public final ForgeConfigSpec.ConfigValue<String> channelId;

    NexusDCBotConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Configuração para o bot do Discord.").push("discord");

        // Define a opção para o Token do Bot
        botToken = builder
                .comment("O token do seu bot do Discord. Mantenha isso privado!")
                .define("botToken", ""); // Valor padrão é uma string vazia

        // Define a opção para o ID do Canal
        channelId = builder
                .comment("O ID do canal do Discord onde o bot irá operar.")
                .define("channelId", ""); // Valor padrão é uma string vazia

        builder.pop();
    }

    // Cria a instância da configuração e o ForgeConfigSpec
    static {
        Pair<NexusDCBotConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(NexusDCBotConfig::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
}
