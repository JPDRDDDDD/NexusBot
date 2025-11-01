package com.nexusbot;

import com.nexusbot.config.NexusDCBotConfig;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.nexusbot.systems.MonitorCore;
import com.nexusbot.config.ConfigManager;
import com.nexusbot.config.UnicodeEnforcer; // ✅ NOVO
import com.nexusbot.commands.AdminCommands;
import com.nexusbot.commands.PlayerCommands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("nexusbot")
public class NexusBotMod {
    public static final String MOD_ID = "nexusbot";
    public static final Logger LOGGER = LogManager.getLogger();

    private static NexusBotMod instance;
    private MonitorCore monitorCore;
    private ConfigManager configManager;

    public static GatewayDiscordClient botdc;
    private static String canalID;

    public NexusBotMod() {
        instance = this;

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NexusDCBotConfig.COMMON_SPEC);
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("NexusBot inicializando...");
    }

    private void setup(final FMLCommonSetupEvent event) {
        this.configManager = new ConfigManager();
        // ✅ ATIVA UNICODE PARA EMOJIS
        UnicodeEnforcer.enableUnicodeFont();

        this.monitorCore = new MonitorCore();

        LOGGER.info("NexusBot carregado com sucesso!");

        // ✅ VERIFICA SE UNICODE ESTÁ ATIVADO
        if (UnicodeEnforcer.isUnicodeEnabled()) {
            LOGGER.info("✅ Sistema de emojis ativado - Todos os emojis devem funcionar!");
        } else {
            LOGGER.warn("⚠ Sistema de emojis pode ter problemas - Unicode não detectado");
        }
        canalID = NexusDCBotConfig.COMMON.channelId.get();

    }
    //BOT DISCORD
    private void start(final FMLServerStartingEvent event){
        String token = NexusDCBotConfig.COMMON.botToken.get();
        iniciarBotDc(token);
    }
    private void stop(final FMLServerStoppingEvent event){
        if(botdc!=null){
            botdc.getChannelById(Snowflake.of(canalID))
                    .ofType(MessageChannel.class)
                    .flatMap(channel -> channel.createMessage(":red_circle: **Servidor está off!**")).subscribe();
            botdc.logout().block();
        }
    }
    private void iniciarBotDc(String token){
        try {
            botdc = DiscordClientBuilder.create(token).build().login().block();
            if (botdc == null) {
                LOGGER.error("Integração com o Discord falhou!");
            }
            botdc.getChannelById(Snowflake.of(canalID)).ofType(MessageChannel.class).flatMap(channel -> channel.createMessage(":green_circle: **Servidor está on**")).subscribe();
            botdc.on(MessageCreateEvent.class).subscribe(event -> {
                if (event.getMessage().getAuthor().map(User::isBot).orElse(true)) return;
                String canal = event.getMessage().getChannelId().asString();
                if (!canal.equals(canalID)) return;
                String autor = event.getMessage().getAuthor().map(User::getUsername).orElse("Desconhecido");
                String msg = event.getMessage().getContent();
            });
        } finally {

        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        AdminCommands.register(event.getDispatcher());
        PlayerCommands.register(event.getDispatcher());
    }

    public static NexusBotMod getInstance() {
        return instance;
    }

    public MonitorCore getMonitorCore() {
        return monitorCore;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}