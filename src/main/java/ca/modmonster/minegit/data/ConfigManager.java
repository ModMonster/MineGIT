package ca.modmonster.minegit.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import ca.modmonster.minegit.MineGIT;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("minegit.json");
    private static Config currentConfig = null;

    public static void save(Config config) {
        currentConfig = config;
        try (FileWriter writer = new FileWriter(configPath.toFile())) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config", e);
        }
    }

    private static Config load() {
        if (!configPath.toFile().exists()) {
            MineGIT.LOGGER.info("No configuration file found: starting a new one!");
            return new Config();
        }

        try (FileReader reader = new FileReader(configPath.toFile())) {
            return GSON.fromJson(reader, Config.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config", e);
        }
    }

    public static Config getCurrentConfig() {
        if (currentConfig != null) return currentConfig;
        currentConfig = load();
        return currentConfig;
    }
}