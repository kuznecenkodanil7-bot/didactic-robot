package io.github.labychat.render;

import io.github.labychat.chat.ChatManager;
import io.github.labychat.chat.ChatMessage;
import io.github.labychat.chat.StyledTextUtil;
import io.github.labychat.config.ConfigManager;
import io.github.labychat.config.LabyChatConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class ChatRenderer {
    private static final int PADDING = 4;
    private final ChatManager chatManager;
    private final ConfigManager configManager;
    private final ContextMenu contextMenu = new ContextMenu();
    private final List<RenderedLine> hitLines = new ArrayList<>();
    private int scrollMessages;

    public ChatRenderer(ChatManager chatManager, ConfigManager configManager) {
        this.chatManager = chatManager;
        this.configManager = configManager;
    }

    public void render(DrawContext context, TextRenderer renderer, int mouseX, int mouseY, boolean interactable) {
        LabyChatConfig config = configManager.get();
        if (!config.enabled) return;
        List<ChatMessage> messages = chatManager.snapshot();
        if (messages.isEmpty()) {
            hitLines.clear();
            return;
        }

        float scale = config.fontScale;
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int logicalWidth = Math.max(100, Math.round(config.width / scale));
        int logicalHeight = Math.max(50, Math.round(config.height / scale));
        int x = switch (config.position) {
            case LEFT_BOTTOM -> 4;
            case RIGHT_BOTTOM -> Math.max(4, Math.round(screenWidth / scale) - logicalWidth - 4);
            case CUSTOM -> config.customX;
        };
        int bottom = switch (config.position) {
            case LEFT_BOTTOM, RIGHT_BOTTOM -> Math.round((screenHeight - 40) / scale);
            case CUSTOM -> config.customY;
        };
        int top = bottom - logicalHeight;

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);
        context.enableScissor(Math.round(x * scale), Math.round(top * scale),
                Math.round((x + logicalWidth) * scale), Math.round(bottom * scale));
        hitLines.clear();

        List<ChatMessage> selected = selectMessages(messages, scrollMessages);
        int cursorY = bottom - PADDING;
        int lineHeight = renderer.fontHeight + 3;
        int visibleRow = 0;
        for (int messageIndex = selected.size() - 1; messageIndex >= 0; messageIndex--) {
            ChatMessage message = selected.get(messageIndex);
            ChatMessage previousOlder = messageIndex > 0 ? selected.get(messageIndex - 1) : null;
            boolean grouped = isGrouped(previousOlder, message, config);
            Text visual = buildVisualText(message, grouped, config);
            int avatarSpace = config.showAvatars && !grouped && message.sender() != null ? config.avatarSize + 4 : 0;
            int indent = grouped ? config.groupingIndent : 0;
            int textX = x + PADDING + avatarSpace + indent;
            int wrapWidth = Math.max(20, logicalWidth - (textX - x) - PADDING - (config.showScrollbar ? 4 : 0));
            List<OrderedText> wrapped = renderer.wrapLines(visual, wrapWidth);
            if (wrapped.isEmpty()) wrapped = List.of(OrderedText.EMPTY);
            int textBlockHeight = wrapped.size() * lineHeight + 2;
            int blockHeight = config.showAvatars && !grouped && message.sender() != null
                    ? Math.max(textBlockHeight, config.avatarSize + 4)
                    : textBlockHeight;
            int blockTop = cursorY - blockHeight;
            if (blockTop < top) break;

            float opacity = interactable ? 1.0f : ageOpacity(message, config);
            if (opacity <= 0.01f) continue;
            int animationOffset = animationOffset(message, config, opacity);
            int rowColor = config.alternateRows && (visibleRow & 1) == 1 ? config.alternateRowColor : config.backgroundColor;
            int background = multiplyAlpha(rowColor, config.backgroundOpacity * opacity);
            context.fill(x, blockTop + animationOffset, x + logicalWidth, cursorY + animationOffset, background);
            if (message.mention()) {
                context.fill(x, blockTop + animationOffset, x + 3, cursorY + animationOffset,
                        multiplyAlpha(config.mentionColor, opacity));
            }

            if (config.showAvatars && !grouped && message.sender() != null) {
                drawAvatar(context, message.sender(), x + PADDING, blockTop + 2 + animationOffset,
                        Math.min(config.avatarSize, blockHeight - 4), opacity);
            }

            int senderStartX = -1;
            int senderEndX = -1;
            if (!grouped && message.sender() != null) {
                String plainVisual = visual.getString();
                int nameCharStart = plainVisual.indexOf(message.sender());
                if (nameCharStart >= 0) {
                    int nameCodePointStart = plainVisual.codePointCount(0, nameCharStart);
                    int nameCodePointEnd = nameCodePointStart + message.sender().codePointCount(0, message.sender().length());
                    senderStartX = textX + renderer.getWidth(StyledTextUtil.takeCodePoints(visual, nameCodePointStart));
                    senderEndX = senderStartX + renderer.getWidth(
                            StyledTextUtil.sliceCodePoints(visual, nameCodePointStart, nameCodePointEnd));
                }
            }

            for (int lineIndex = 0; lineIndex < wrapped.size(); lineIndex++) {
                OrderedText line = wrapped.get(lineIndex);
                int lineY = blockTop + 1 + lineIndex * lineHeight + animationOffset;
                int color = multiplyAlpha(0xFFFFFFFF, opacity);
                context.drawTextWithShadow(renderer, line, textX, lineY, color);
                hitLines.add(new RenderedLine(textX, lineY, renderer.getWidth(line), lineHeight,
                        line, message, lineIndex == 0, textX,
                        lineIndex == 0 ? senderStartX : -1, lineIndex == 0 ? senderEndX : -1));
            }
            cursorY = blockTop - 1;
            visibleRow++;
        }

        if (config.showScrollbar && !messages.isEmpty()) {
            drawScrollbar(context, x, top, bottom, logicalWidth, messages.size(), selected.size());
        }
        context.disableScissor();
        context.getMatrices().popMatrix();
    }

    public void renderPreview(DrawContext context, TextRenderer renderer, int x, int y, int width, int height) {
        LabyChatConfig config = configManager.get();
        context.fill(x, y, x + width, y + height, multiplyAlpha(config.backgroundColor, config.backgroundOpacity));
        context.drawStrokedRectangle(x, y, width, height, 0x885A6575);
        List<Text> samples = List.of(
                Text.literal("[12:41] <Alex> Привет! Это живое превью 😀"),
                Text.literal("[12:41]     Ещё одно сгруппированное сообщение"),
                Text.literal("[12:42] [VIP] Steve: @Player, проверь ссылку")
        );
        int drawY = y + height - 16;
        for (int i = samples.size() - 1; i >= 0; i--) {
            context.drawTextWithShadow(renderer, samples.get(i), x + 6, drawY, 0xFFFFFFFF);
            drawY -= renderer.fontHeight + 5;
        }
    }

    private List<ChatMessage> selectMessages(List<ChatMessage> messages, int offset) {
        if (messages.isEmpty()) return List.of();
        int end = Math.max(0, messages.size() - Math.max(0, offset));
        int start = Math.max(0, end - configManager.get().maximumLines);
        return new ArrayList<>(messages.subList(start, end));
    }

    private Text buildVisualText(ChatMessage message, boolean grouped, LabyChatConfig config) {
        Text body = grouped ? StyledTextUtil.dropCodePoints(message.display(), message.senderPrefixCodePoints())
                : message.display().copy();
        String timestamp = formatTime(message, config.timeFormat);
        var result = Text.empty();
        if (!timestamp.isEmpty()) result.append(Text.literal("[" + timestamp + "] ").styled(style -> style.withColor(0x9CA3AF)));
        result.append(body);
        if (message.duplicateCount() > 1) {
            result.append(Text.literal(" ×" + message.duplicateCount()).styled(style -> style.withColor(0xFBBF24)));
        }
        return result;
    }

    private String formatTime(ChatMessage message, String format) {
        if (format == null || format.isBlank() || "off".equalsIgnoreCase(format)) return "";
        try {
            return DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault()).format(message.receivedAt());
        } catch (RuntimeException ignored) {
            return DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(message.receivedAt());
        }
    }

    private boolean isGrouped(@Nullable ChatMessage previousOlder, ChatMessage currentNewer, LabyChatConfig config) {
        if (previousOlder == null || previousOlder.sender() == null || currentNewer.sender() == null) return false;
        if (!previousOlder.sender().equalsIgnoreCase(currentNewer.sender())) return false;
        long seconds = Duration.between(previousOlder.receivedAt(), currentNewer.receivedAt()).toSeconds();
        return seconds >= 0 && seconds <= config.groupingWindowSeconds;
    }

    private float ageOpacity(ChatMessage message, LabyChatConfig config) {
        long ageMillis = Duration.between(message.receivedAt(), Instant.now()).toMillis();
        long stay = config.disappearDelaySeconds * 1000L;
        if (ageMillis <= stay) return appearanceProgress(message, config);
        long fade = Math.max(150L, config.animationMillis);
        return clamp01(1.0f - (ageMillis - stay) / (float) fade);
    }

    private float appearanceProgress(ChatMessage message, LabyChatConfig config) {
        if (config.animation == LabyChatConfig.Animation.NONE || config.animationMillis <= 0) return 1.0f;
        long ageMillis = Math.max(0L, (System.nanoTime() - message.receivedNanos()) / 1_000_000L);
        return clamp01(ageMillis / (float) config.animationMillis);
    }

    private int animationOffset(ChatMessage message, LabyChatConfig config, float opacity) {
        if (config.animation != LabyChatConfig.Animation.SLIDE) return 0;
        return Math.round((1.0f - appearanceProgress(message, config)) * 10.0f);
    }

    private void drawAvatar(DrawContext context, String sender, int x, int y, int size, float opacity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null || size <= 0) return;
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(sender);
        if (entry == null) return;
        try {
            PlayerSkinDrawer.draw(context, entry.getSkinTextures(), x, y, size, multiplyAlpha(0xFFFFFFFF, opacity));
            context.fill(x + size - 4, y + size - 4, x + size, y + size, 0xFF4ADE80);
        } catch (RuntimeException ignored) {
            // Vanilla's skin cache is asynchronous. A missing texture is simply rendered next frame.
        }
    }

    private void drawScrollbar(DrawContext context, int x, int top, int bottom, int width, int total, int visible) {
        if (total <= visible || total <= 0) return;
        int trackX = x + width - 3;
        int trackHeight = bottom - top;
        int thumbHeight = Math.max(8, Math.round(trackHeight * (visible / (float) total)));
        int maxOffset = Math.max(1, total - visible);
        int thumbY = top + Math.round((trackHeight - thumbHeight) * (scrollMessages / (float) maxOffset));
        context.fill(trackX, top, trackX + 2, bottom, 0x44202020);
        context.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xBBD1D5DB);
    }

    public boolean scroll(double verticalAmount) {
        List<ChatMessage> messages = chatManager.snapshot();
        if (messages.isEmpty()) return false;
        int delta = verticalAmount > 0 ? 3 : verticalAmount < 0 ? -3 : 0;
        if (delta == 0) return false;
        scrollMessages = Math.max(0, Math.min(Math.max(0, messages.size() - 1), scrollMessages + delta));
        return true;
    }

    public void resetScroll() {
        scrollMessages = 0;
    }

    public @Nullable String senderAt(double mouseX, double mouseY) {
        float scale = configManager.get().fontScale;
        double logicalX = mouseX / scale;
        double logicalY = mouseY / scale;
        for (RenderedLine line : hitLines) {
            if (line.containsSender(logicalX, logicalY)) return line.message().sender();
        }
        return null;
    }

    public @Nullable Style styleAt(double mouseX, double mouseY, TextRenderer renderer) {
        float scale = configManager.get().fontScale;
        double logicalX = mouseX / scale;
        double logicalY = mouseY / scale;
        for (RenderedLine line : hitLines) {
            if (!line.contains(logicalX, logicalY)) continue;
            int target = Math.max(0, (int) logicalX - line.messageTextX());
            final int[] width = {0};
            final Style[] found = {null};
            line.text().accept((index, style, codePoint) -> {
                int glyphWidth = renderer.getWidth(OrderedText.styled(codePoint, style));
                if (target >= width[0] && target < width[0] + glyphWidth) {
                    found[0] = style;
                    return false;
                }
                width[0] += glyphWidth;
                return true;
            });
            return found[0];
        }
        return null;
    }

    public ContextMenu contextMenu() {
        return contextMenu;
    }

    private static int multiplyAlpha(int argb, float opacity) {
        int alpha = (argb >>> 24) & 0xFF;
        int adjusted = Math.max(0, Math.min(255, Math.round(alpha * clamp01(opacity))));
        return (adjusted << 24) | (argb & 0x00FFFFFF);
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
