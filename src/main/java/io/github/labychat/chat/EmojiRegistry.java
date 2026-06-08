package io.github.labychat.chat;

import io.github.labychat.LabyChatClient;
import io.github.labychat.api.LabyChatAPI;
import io.github.labychat.config.ConfigManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EmojiRegistry {
    private final ConfigManager configManager;
    private final Map<String, String> replacements = new LinkedHashMap<>();
    private final List<CustomEmoji> customEmojis = new ArrayList<>();

    public EmojiRegistry(ConfigManager configManager) {
        this.configManager = configManager;
        registerDefaults();
    }

    private void registerDefaults() {
        replacements.put(":shrug:", "¯\\_(ツ)_/¯");
        replacements.put(":heart:", "❤️");
        replacements.put(":smile:", "😊");
        replacements.put(":laugh:", "😂");
        replacements.put(":wave:", "👋");
        replacements.put(":fire:", "🔥");
        replacements.put(":check:", "✅");
        replacements.put(":x:", "❌");
        replacements.put(":thinking:", "🤔");
        replacements.put(":party:", "🎉");
    }

    public void scanCustom() {
        customEmojis.clear();
        Path directory = configManager.emojiDirectory();
        try {
            Files.createDirectories(directory);
            try (var paths = Files.list(directory)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .forEach(path -> {
                            String fileName = path.getFileName().toString();
                            String name = fileName.substring(0, fileName.length() - 4)
                                    .replaceAll("[^A-Za-z0-9_\\-]", "_");
                            if (!name.isBlank()) customEmojis.add(new CustomEmoji(":" + name + ":", path));
                        });
            }
        } catch (Exception exception) {
            LabyChatClient.LOGGER.warn("Could not scan custom emojis", exception);
        }
    }

    public Text replaceIncoming(Text input) {
        return StyledTextUtil.mapSegments(input, this::replace);
    }

    public String replaceOutgoing(String input) {
        return replace(input);
    }

    private String replace(String input) {
        String value = input;
        if (configManager.get().replaceNamedEmoji) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                value = value.replace(entry.getKey(), entry.getValue());
            }
        }
        if (configManager.get().replaceAsciiEmoticons) {
            value = value.replaceAll("(?<![:;])(?<!\\S):\\)(?!\\S)", "😊")
                    .replaceAll("(?<!\\S):D(?!\\S)", "😄")
                    .replaceAll("(?<!\\S);\\)(?!\\S)", "😉");
        }
        return value;
    }

    public List<EmojiChoice> choices() {
        List<EmojiChoice> result = new ArrayList<>();
        replacements.forEach((shortcut, unicode) -> result.add(new EmojiChoice(shortcut, unicode, null, null)));
        for (CustomEmoji custom : customEmojis) {
            result.add(new EmojiChoice(custom.shortcut(), custom.shortcut(), custom.path(), null));
        }
        LabyChatAPI.emojis().forEach((shortcut, texture) ->
                result.add(new EmojiChoice(shortcut, shortcut, null, texture)));
        return result;
    }

    public record CustomEmoji(String shortcut, Path path) {
    }

    public record EmojiChoice(String shortcut, String insertion, Path externalTexture, Identifier resourceTexture) {
    }
}
