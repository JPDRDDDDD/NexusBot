package com.nexusbot;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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

    public NexusBotMod() {
        instance = this;

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
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