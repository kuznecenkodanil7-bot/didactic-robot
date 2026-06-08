package io.github.labychat.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LabyChatConfig {
    public int schemaVersion = 1;

    public boolean enabled = true;
    public int width = 420;
    public int height = 220;
    public float backgroundOpacity = 0.60f;
    public float fontScale = 1.0f;
    public int backgroundColor = 0xA0101010;
    public int alternateRowColor = 0x18000000;
    public boolean alternateRows = false;
    public Position position = Position.LEFT_BOTTOM;
    public int customX = 4;
    public int customY = 40;
    public Animation animation = Animation.SLIDE;
    public int animationMillis = 220;
    public int disappearDelaySeconds = 10;
    public int maximumLines = 500;
    public int maximumMessages = 10_000;
    public String timeFormat = "HH:mm";
    public int groupingIndent = 18;
    public int groupingWindowSeconds = 90;
    public boolean showAvatars = true;
    public int avatarSize = 16;
    public boolean showScrollbar = true;
    public boolean notificationSound = true;
    public String notificationSoundId = "minecraft:entity.experience_orb.pickup";
    public String notificationSoundFile = "";
    public boolean mentionHighlight = true;
    public int mentionColor = 0x66FFD54F;
    public boolean antiSpam = true;
    public int antiSpamWindowSeconds = 8;
    public boolean replaceAsciiEmoticons = true;
    public boolean replaceNamedEmoji = true;
    public boolean logMessages = true;
    public int logRetentionMessages = 10_000;
    public String theme = "labymod_classic";
    public String privateMessageCommand = "auto";
    public boolean autoLinkify = true;
    public boolean linkTitlePreview = false;
    public boolean translationEnabled = false;
    public String libreTranslateEndpoint = "";
    public String libreTranslateApiKey = "";
    public String translateTargetLanguage = "en";
    public boolean outgoingQuickRepliesEnabled = true;

    public List<String> blockedPlayers = new ArrayList<>();
    public List<String> regexFilters = new ArrayList<>();
    public Map<String, String> quickReplies = new LinkedHashMap<>();

    public enum Position {
        LEFT_BOTTOM,
        RIGHT_BOTTOM,
        CUSTOM
    }

    public enum Animation {
        NONE,
        SLIDE,
        FADE,
        SCALE
    }

    public void clamp() {
        width = clamp(width, 100, 640);
        height = clamp(height, 50, 1000);
        backgroundOpacity = clamp(backgroundOpacity, 0.0f, 1.0f);
        fontScale = clamp(fontScale, 0.5f, 2.0f);
        animationMillis = clamp(animationMillis, 0, 2000);
        disappearDelaySeconds = clamp(disappearDelaySeconds, 1, 60);
        maximumLines = clamp(maximumLines, 20, 500);
        maximumMessages = clamp(maximumMessages, 20, 10_000);
        groupingIndent = clamp(groupingIndent, 0, 50);
        groupingWindowSeconds = clamp(groupingWindowSeconds, 0, 600);
        avatarSize = clamp(avatarSize, 8, 32);
        antiSpamWindowSeconds = clamp(antiSpamWindowSeconds, 1, 120);
        logRetentionMessages = clamp(logRetentionMessages, 100, 10_000);
        if (position == null) position = Position.LEFT_BOTTOM;
        if (animation == null) animation = Animation.SLIDE;
        if (timeFormat == null) timeFormat = "HH:mm";
        if (theme == null || theme.isBlank()) theme = "labymod_classic";
        if (privateMessageCommand == null || privateMessageCommand.isBlank()) privateMessageCommand = "auto";
        if (blockedPlayers == null) blockedPlayers = new ArrayList<>();
        if (regexFilters == null) regexFilters = new ArrayList<>();
        if (quickReplies == null) quickReplies = new LinkedHashMap<>();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
