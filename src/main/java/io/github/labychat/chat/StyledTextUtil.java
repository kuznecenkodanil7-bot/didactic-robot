package io.github.labychat.chat;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.UnaryOperator;

public final class StyledTextUtil {
    private static final Pattern URL = Pattern.compile("(?i)\\bhttps?://[^\\s<>{}\\[\\]]+");
    private StyledTextUtil() {
    }

    public static Text mapSegments(Text input, UnaryOperator<String> mapper) {
        MutableText output = Text.empty();
        input.visit((style, segment) -> {
            String mapped = mapper.apply(segment);
            if (!mapped.isEmpty()) output.append(Text.literal(mapped).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return output;
    }

    public static Text linkify(Text input) {
        MutableText output = Text.empty();
        input.visit((style, segment) -> {
            if (style.getClickEvent() != null) {
                output.append(Text.literal(segment).setStyle(style));
                return Optional.empty();
            }
            Matcher matcher = URL.matcher(segment);
            int cursor = 0;
            while (matcher.find()) {
                if (matcher.start() > cursor) {
                    output.append(Text.literal(segment.substring(cursor, matcher.start())).setStyle(style));
                }
                String url = stripTrailingPunctuation(matcher.group());
                int retained = matcher.group().length() - url.length();
                try {
                    Style linkStyle = style
                            .withColor(0x55AAFF)
                            .withUnderline(true)
                            .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)));
                    if (style.getHoverEvent() == null) {
                        linkStyle = linkStyle.withHoverEvent(new HoverEvent.ShowText(Text.literal(url)));
                    }
                    output.append(Text.literal(url).setStyle(linkStyle));
                } catch (IllegalArgumentException ignored) {
                    output.append(Text.literal(url).setStyle(style));
                }
                if (retained > 0) {
                    output.append(Text.literal(matcher.group().substring(url.length())).setStyle(style));
                }
                cursor = matcher.end();
            }
            if (cursor < segment.length()) output.append(Text.literal(segment.substring(cursor)).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return output;
    }

    private static String stripTrailingPunctuation(String value) {
        int end = value.length();
        while (end > 0 && ".,;:!?)]}".indexOf(value.charAt(end - 1)) >= 0) end--;
        return value.substring(0, end);
    }

    public static Text dropCodePoints(Text input, int amount) {
        if (amount <= 0) return input.copy();
        MutableText output = Text.empty();
        int[] remaining = {amount};
        input.visit((style, segment) -> {
            if (remaining[0] <= 0) {
                output.append(Text.literal(segment).setStyle(style));
                return Optional.empty();
            }
            int count = segment.codePointCount(0, segment.length());
            if (count <= remaining[0]) {
                remaining[0] -= count;
                return Optional.empty();
            }
            int charIndex = segment.offsetByCodePoints(0, remaining[0]);
            remaining[0] = 0;
            output.append(Text.literal(segment.substring(charIndex)).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return output;
    }
    public static Text takeCodePoints(Text input, int amount) {
        if (amount <= 0) return Text.empty();
        MutableText output = Text.empty();
        int[] remaining = {amount};
        input.visit((style, segment) -> {
            if (remaining[0] <= 0) return Optional.of(Boolean.TRUE);
            int count = segment.codePointCount(0, segment.length());
            if (count <= remaining[0]) {
                output.append(Text.literal(segment).setStyle(style));
                remaining[0] -= count;
                return Optional.empty();
            }
            int charIndex = segment.offsetByCodePoints(0, remaining[0]);
            output.append(Text.literal(segment.substring(0, charIndex)).setStyle(style));
            remaining[0] = 0;
            return Optional.of(Boolean.TRUE);
        }, Style.EMPTY);
        return output;
    }

    public static Text sliceCodePoints(Text input, int start, int end) {
        if (end <= start) return Text.empty();
        return takeCodePoints(dropCodePoints(input, Math.max(0, start)), end - Math.max(0, start));
    }

}
