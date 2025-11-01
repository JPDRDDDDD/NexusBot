package com.nexusbot.config;

import com.nexusbot.NexusBotMod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class UnicodeEnforcer {

    private static final String[] MINECRAFT_PATHS = {
            "config/options.txt",
            "options.txt",
            "../config/options.txt",
            "../options.txt"
    };

    /**
     * Força o Minecraft a usar fontes Unicode para suportar todos os emojis
     */
    public static void enableUnicodeFont() {
        NexusBotMod.LOGGER.info("🔧 Iniciando ativação de Unicode para emojis...");

        boolean success = false;
        for (String path : MINECRAFT_PATHS) {
            if (enableUnicodeInFile(path)) {
                success = true;
                break;
            }
        }

        if (success) {
            NexusBotMod.LOGGER.info("✅ Unicode ativado com sucesso! Todos os emojis devem funcionar.");
        } else {
            NexusBotMod.LOGGER.warn("⚠ Não foi possível encontrar options.txt para ativar Unicode");
        }
    }

    private static boolean enableUnicodeInFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return false;
            }

            List<String> lines = Files.readAllLines(path);
            List<String> newLines = new ArrayList<>();
            boolean found = false;

            for (String line : lines) {
                if (line.startsWith("forceUnicodeFont:")) {
                    newLines.add("forceUnicodeFont:true");
                    found = true;
                    NexusBotMod.LOGGER.info("🔧 Unicode alterado para: true");
                } else {
                    newLines.add(line);
                }
            }

            if (!found) {
                newLines.add("forceUnicodeFont:true");
                NexusBotMod.LOGGER.info("🔧 Unicode adicionado: true");
            }

            Files.write(path, newLines);
            return true;

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao ativar Unicode em {}: {}", filePath, e.getMessage());
            return false;
        }
    }

    /**
     * Verifica se o Unicode está ativado
     */
    public static boolean isUnicodeEnabled() {
        for (String path : MINECRAFT_PATHS) {
            try {
                Path filePath = Paths.get(path);
                if (Files.exists(filePath)) {
                    List<String> lines = Files.readAllLines(filePath);
                    for (String line : lines) {
                        if (line.startsWith("forceUnicodeFont:true")) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                // Ignora erros de leitura
            }
        }
        return false;
    }
}