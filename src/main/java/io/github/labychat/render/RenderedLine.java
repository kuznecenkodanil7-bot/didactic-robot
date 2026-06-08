package io.github.labychat.render;

import io.github.labychat.chat.ChatMessage;
import net.minecraft.text.OrderedText;

public record RenderedLine(
        int x,
        int y,
        int width,
        int height,
        OrderedText text,
        ChatMessage message,
        boolean firstLine,
        int messageTextX,
        int senderStartX,
        int senderEndX
) {
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean containsSender(double mouseX, double mouseY) {
        return firstLine && senderStartX >= 0 && mouseX >= senderStartX && mouseX < senderEndX
                && mouseY >= y && mouseY < y + height;
    }
}
