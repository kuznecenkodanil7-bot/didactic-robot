package io.github.labychat.chat;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageParser {
    private static final Pattern[] PLAYER_PATTERNS = {
            Pattern.compile("^(?:\\[[^]\\r\\n]{1,32}]\\s*)*<([A-Za-z0-9_]{3,16})>\\s*"),
            Pattern.compile("^(?:\\[[^]\\r\\n]{1,32}]\\s*)*([A-Za-z0-9_]{3,16})\\s*(?::|»|➜|>)\\s*"),
            Pattern.compile("^(?:\\([^)]{1,32}\\)\\s*)*(?:\\[[^]\\r\\n]{1,32}]\\s*)*([A-Za-z0-9_]{3,16})\\s*(?::|»|➜|>)\\s*")
    };

    private MessageParser() {
    }

    public static ParsedSender parse(String plainText) {
        for (Pattern pattern : PLAYER_PATTERNS) {
            Matcher matcher = pattern.matcher(plainText);
            if (matcher.find()) {
                return new ParsedSender(matcher.group(1), plainText.codePointCount(0, matcher.end()));
            }
        }
        return new ParsedSender(null, 0);
    }

    public record ParsedSender(@Nullable String name, int prefixCodePoints) {
    }
}
