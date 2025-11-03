package com.nexusbot;

import com.nexusbot.config.NexusDCBotConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
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

import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

@Mod("nexusbot")
public class NexusBotMod {
    public static final String MOD_ID = "nexusbot";
    public static final Logger LOGGER = LogManager.getLogger();

    private static NexusBotMod instance;
    private MonitorCore monitorCore;
    private ConfigManager configManager;

    public static JDA botDC;
    public static String canalID;

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
    @SubscribeEvent
    public void onServerStart(final FMLServerStartingEvent event){
        String token = NexusDCBotConfig.COMMON.botToken.get();
        new Thread(() -> iniciarBotDc(token), "NexusBot-DiscordThread").start();
    }
    @SubscribeEvent
    public void onServerStop(final FMLServerStoppingEvent event){
        String alert = ":red_circle: **Servidor Desligou**";
        if (botDC == null || botDC.getStatus() != JDA.Status.CONNECTED) return;

        TextChannel canal = botDC.getTextChannelById(canalID);
        if (canal != null) {
            canal.sendMessage(alert).queue();
        }
    }
    private void iniciarBotDc(String token){
        try {
            if (token.isEmpty()) {
                LOGGER.error("Integração com o Discord falhou!");
            }
            JDABuilder builder = JDABuilder.createDefault(token); // 4.x não aceita intents no createDefault
            // habilita os intents necessários
            builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES);
            builder.addEventListeners(new DiscordListener());

            botDC = builder.build();
            botDC.awaitReady();

            String alert = ":green_circle: **Servidor Ligou**";
            TextChannel canal = botDC.getTextChannelById(canalID);
            if (canal != null) {
                canal.sendMessage(alert).queue();
            }
            LOGGER.info("[NexusBot] Bot do Discord iniciado!");
        } catch (Exception e) {
            LOGGER.error("[NexusBot] erro ao realizar a integração");
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

    public static class DiscordListener extends ListenerAdapter {
        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event.getAuthor().isBot()) return;
            if (!event.getChannel().getId().equals(canalID)) return;

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.execute(() -> server.getPlayerList().broadcastMessage(
                        new StringTextComponent("§9[Discord] §r" + event.getAuthor().getName() + ": " + event.getMessage().getContentRaw()),
                        ChatType.CHAT,
                        UUID.randomUUID()
                ));
            }
        }
    }
    @SubscribeEvent
    public static void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();

        if (event.getNewGameMode() == GameType.CREATIVE) {
            String alert = "⚠️ **" + player.getName().getString() + "** entrou no modo criativo!";
            if (botDC == null || botDC.getStatus() != JDA.Status.CONNECTED) return;
            TextChannel canal = botDC.getTextChannelById(canalID);
            if (canal != null) {
                canal.sendMessage(alert).queue();
            }
            LOGGER.warn(alert);
        }
    }
}