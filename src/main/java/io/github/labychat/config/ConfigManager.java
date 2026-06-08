package io.github.labychat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.labychat.LabyChatClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path root;
    private final Path configFile;
    private LabyChatConfig config;

    public ConfigManager() {
        this.root = FabricLoader.getInstance().getConfigDir().resolve("LabyChat");
        this.configFile = root.resolve("config.json");
    }

    public void load() {
        try {
            Files.createDirectories(root);
            Files.createDirectories(root.resolve("emojis"));
            Files.createDirectories(root.resolve("themes"));
            if (Files.isRegularFile(configFile)) {
                try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                    config = GSON.fromJson(reader, LabyChatConfig.class);
                }
            }
        } catch (Exception exception) {
            LabyChatClient.LOGGER.error("Could not load LabyChat config", exception);
        }
        if (config == null) config = new LabyChatConfig();
        config.clamp();
        save();
    }

    public synchronized void save() {
        config.clamp();
        try {
            Files.createDirectories(root);
            Path temp = configFile.resolveSibling(configFile.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
            try {
                Files.move(temp, configFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temp, configFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LabyChatClient.LOGGER.error("Could not save LabyChat config", exception);
        }
    }

    public LabyChatConfig get() {
        return config;
    }

    public Path root() {
        return root;
    }

    public Path emojiDirectory() {
        return root.resolve("emojis");
    }

    public Path themeDirectory() {
        return root.resolve("themes");
    }
}
