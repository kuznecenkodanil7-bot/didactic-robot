package io.github.labychat.chat;

import io.github.labychat.LabyChatClient;
import io.github.labychat.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ChatLogWriter implements AutoCloseable {
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter LINE_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Path directory = FabricLoader.getInstance().getGameDir().resolve("logs/labychat");
    private final ConfigManager configManager;
    private int writesSincePrune;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "LabyChat-LogWriter");
        thread.setDaemon(true);
        return thread;
    });

    public ChatLogWriter(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void append(ChatMessage message) {
        String plain = message.display().getString();
        executor.execute(() -> writeLine(message, plain));
    }

    private void writeLine(ChatMessage message, String plain) {
        try {
            Files.createDirectories(directory);
            LocalDate day = LocalDate.ofInstant(message.receivedAt(), ZoneId.systemDefault());
            Path file = directory.resolve(FILE_DATE.format(day) + ".log");
            String time = LINE_TIME.format(message.receivedAt().atZone(ZoneId.systemDefault()));
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                writer.write('[' + time + "] " + plain.replace('\n', ' ').replace('\r', ' '));
                writer.newLine();
            }
            if (++writesSincePrune >= 128) {
                writesSincePrune = 0;
                prune(configManager.get().logRetentionMessages);
            }
        } catch (IOException exception) {
            LabyChatClient.LOGGER.warn("Could not write chat log", exception);
        }
    }

    private void prune(int retention) throws IOException {
        if (!Files.isDirectory(directory)) return;
        List<Path> files;
        try (var stream = Files.list(directory)) {
            files = stream.filter(path -> path.getFileName().toString().endsWith(".log"))
                    .sorted(java.util.Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
        List<List<String>> contents = new ArrayList<>(files.size());
        int total = 0;
        for (Path file : files) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            contents.add(lines);
            total += lines.size();
        }
        int excess = total - Math.max(100, Math.min(10_000, retention));
        for (int i = 0; i < files.size() && excess > 0; i++) {
            Path file = files.get(i);
            List<String> lines = contents.get(i);
            if (excess >= lines.size()) {
                excess -= lines.size();
                Files.deleteIfExists(file);
            } else {
                Files.write(file, lines.subList(excess, lines.size()), StandardCharsets.UTF_8,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                excess = 0;
            }
        }
    }

    public List<String> search(String query, int limit) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        if (!Files.isDirectory(directory)) return result;
        try (var files = Files.list(directory)) {
            List<Path> ordered = files.filter(path -> path.getFileName().toString().endsWith(".log"))
                    .sorted(ComparatorByFileName.REVERSED)
                    .toList();
            for (Path file : ordered) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (needle.isBlank() || line.toLowerCase(Locale.ROOT).contains(needle)) {
                        result.add(line);
                        if (result.size() >= limit) return result;
                    }
                }
            }
        } catch (IOException exception) {
            LabyChatClient.LOGGER.warn("Could not search chat logs", exception);
        }
        return result;
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    private static final class ComparatorByFileName {
        private static final java.util.Comparator<Path> REVERSED =
                java.util.Comparator.comparing((Path path) -> path.getFileName().toString()).reversed();
    }
}
