package io.github.labychat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.labychat.LabyChatClient;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ThemeManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String[] BUILT_INS = {"labymod_classic", "modern_flat", "transparent_minimal"};
    private final ConfigManager configManager;
    private final Map<String, Theme> themes = new LinkedHashMap<>();

    public ThemeManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void load() {
        themes.clear();
        for (String id : BUILT_INS) {
            String resource = "/labychat/themes/" + id + ".json";
            try (Reader reader = new InputStreamReader(
                    java.util.Objects.requireNonNull(getClass().getResourceAsStream(resource)), StandardCharsets.UTF_8)) {
                Theme theme = GSON.fromJson(reader, Theme.class);
                themes.put(theme.id, theme);
            } catch (Exception exception) {
                LabyChatClient.LOGGER.error("Could not load built-in theme {}", id, exception);
            }
        }
        Path directory = configManager.themeDirectory();
        try {
            Files.createDirectories(directory);
            try (var paths = Files.list(directory)) {
                paths.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".json"))
                        .forEach(this::loadCustom);
            }
        } catch (Exception exception) {
            LabyChatClient.LOGGER.error("Could not scan custom themes", exception);
        }
    }

    private void loadCustom(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Theme theme = GSON.fromJson(reader, Theme.class);
            if (theme != null && theme.id != null && !theme.id.isBlank()) themes.put(theme.id, theme);
        } catch (Exception exception) {
            LabyChatClient.LOGGER.warn("Skipping invalid theme {}", path, exception);
        }
    }

    public Theme current() {
        Theme theme = themes.get(configManager.get().theme);
        return theme != null ? theme : themes.values().stream().findFirst().orElseGet(Theme::new);
    }

    public Map<String, Theme> all() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(themes));
    }

    public void apply(String id) {
        Theme theme = themes.get(id);
        if (theme == null) return;
        LabyChatConfig config = configManager.get();
        config.theme = id;
        config.backgroundColor = theme.backgroundColor;
        config.alternateRowColor = theme.alternateRowColor;
        config.mentionColor = theme.mentionColor;
        config.backgroundOpacity = theme.backgroundOpacity;
        config.alternateRows = theme.alternateRows;
        config.animation = theme.animation;
        configManager.save();
    }
}
