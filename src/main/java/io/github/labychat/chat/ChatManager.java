package io.github.labychat.chat;

import io.github.labychat.LabyChatClient;
import io.github.labychat.api.ITransformer;
import io.github.labychat.api.LabyChatAPI;
import io.github.labychat.api.TransformContext;
import io.github.labychat.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ChatManager {
    private final ConfigManager configManager;
    private final EmojiRegistry emojiRegistry;
    private final ChatLogWriter logWriter;
    private final Deque<ChatMessage> messages = new ArrayDeque<>();
    private List<Pattern> filters = List.of();
    private List<String> lastFilterSource = List.of();

    public ChatManager(ConfigManager configManager, EmojiRegistry emojiRegistry, ChatLogWriter logWriter) {
        this.configManager = configManager;
        this.emojiRegistry = emojiRegistry;
        this.logWriter = logWriter;
    }

    public synchronized void capture(Text incoming, @Nullable MessageSignatureData signature) {
        MinecraftClient client = MinecraftClient.getInstance();
        Instant now = Instant.now();
        String plain = incoming.getString();
        MessageParser.ParsedSender parsed = MessageParser.parse(plain);

        if (isBlocked(parsed.name()) || matchesFilter(plain)) return;

        Text transformed = emojiRegistry.replaceIncoming(incoming.copy());
        if (configManager.get().autoLinkify) transformed = StyledTextUtil.linkify(transformed);
        TransformContext context = new TransformContext(client, now, parsed.name(), parsed.name() == null);
        for (ITransformer transformer : LabyChatAPI.transformers()) {
            try {
                Optional<Text> result = transformer.transform(transformed, context);
                if (result.isEmpty()) return;
                transformed = result.get();
            } catch (RuntimeException exception) {
                LabyChatClient.LOGGER.warn("A LabyChat message transformer failed", exception);
            }
        }

        boolean mention = isMention(client, transformed.getString(), parsed.name());
        ChatMessage newest = messages.peekLast();
        if (configManager.get().antiSpam && newest != null
                && newest.display().getString().equals(transformed.getString())
                && Duration.between(newest.receivedAt(), now).toSeconds() <= configManager.get().antiSpamWindowSeconds) {
            newest.incrementDuplicateCount();
            if (configManager.get().logMessages) {
                ChatMessage duplicate = new ChatMessage(incoming.copy(), transformed.copy(), parsed.name(),
                        parsed.prefixCodePoints(), now, System.nanoTime(), signature, mention);
                logWriter.append(duplicate);
            }
            if (mention) notifyMention(client);
            return;
        }

        ChatMessage message = new ChatMessage(incoming.copy(), transformed, parsed.name(), parsed.prefixCodePoints(),
                now, System.nanoTime(), signature, mention);
        messages.addLast(message);
        trim();
        if (configManager.get().logMessages) logWriter.append(message);
        if (mention) notifyMention(client);
    }

    private boolean isBlocked(@Nullable String sender) {
        if (sender == null) return false;
        return configManager.get().blockedPlayers.stream().anyMatch(name -> name.equalsIgnoreCase(sender));
    }

    private boolean matchesFilter(String plain) {
        rebuildFiltersIfNeeded();
        for (Pattern pattern : filters) {
            if (pattern.matcher(plain).find()) return true;
        }
        return false;
    }

    private void rebuildFiltersIfNeeded() {
        List<String> source = List.copyOf(configManager.get().regexFilters);
        if (source.equals(lastFilterSource)) return;
        List<Pattern> rebuilt = new ArrayList<>();
        for (String regex : source) {
            try {
                rebuilt.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            } catch (PatternSyntaxException exception) {
                LabyChatClient.LOGGER.warn("Ignoring invalid chat regex: {}", regex);
            }
        }
        filters = List.copyOf(rebuilt);
        lastFilterSource = source;
    }

    private boolean isMention(MinecraftClient client, String plain, @Nullable String sender) {
        if (!configManager.get().mentionHighlight || client.player == null) return false;
        String ownName = client.player.getGameProfile().getName();
        if (sender != null && sender.equalsIgnoreCase(ownName)) return false;
        Pattern mention = Pattern.compile("(?i)(?<![A-Za-z0-9_])@?" + Pattern.quote(ownName) + "(?![A-Za-z0-9_])");
        return mention.matcher(plain).find();
    }

    private void notifyMention(MinecraftClient client) {
        if (!configManager.get().notificationSound || client.player == null) return;
        client.execute(() -> {
            if (client.player != null) {
                client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.75f, 1.25f);
            }
        });
    }

    private void trim() {
        int limit = Math.min(10_000, configManager.get().maximumMessages);
        while (messages.size() > limit) messages.removeFirst();
    }

    public synchronized List<ChatMessage> snapshot() {
        return List.copyOf(messages);
    }

    public synchronized void clear() {
        messages.clear();
    }

    public synchronized void removeBySignature(MessageSignatureData signature) {
        messages.removeIf(message -> signature.equals(message.signature()));
    }

    public String transformOutgoing(String message) {
        return emojiRegistry.replaceOutgoing(message);
    }

    public List<String> searchMemory(String query, int limit) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        synchronized (this) {
            var iterator = messages.descendingIterator();
            while (iterator.hasNext() && result.size() < limit) {
                ChatMessage message = iterator.next();
                String line = message.display().getString();
                if (needle.isBlank() || line.toLowerCase(Locale.ROOT).contains(needle)) result.add(line);
            }
        }
        return result;
    }

    public ChatLogWriter logWriter() {
        return logWriter;
    }
}
