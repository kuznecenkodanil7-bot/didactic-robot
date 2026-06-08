package io.github.labychat.render;

import io.github.labychat.LabyChatClient;
import io.github.labychat.api.ContextMenuContext;
import io.github.labychat.api.IContextMenuEntry;
import io.github.labychat.api.LabyChatAPI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class ContextMenu {
    private final List<Entry> entries = new ArrayList<>();
    private boolean open;
    private int x;
    private int y;
    private int width;
    private String playerName = "";

    public void open(MinecraftClient client, String playerName, int mouseX, int mouseY) {
        this.playerName = playerName;
        this.x = mouseX;
        this.y = mouseY;
        entries.clear();
        ContextMenuContext context = new ContextMenuContext(client, playerName);
        for (var apiEntry : LabyChatAPI.contextMenuEntries().entrySet()) {
            try {
                IContextMenuEntry value = apiEntry.getValue();
                if (value.isVisible(context)) entries.add(new Entry(apiEntry.getKey(), value, value.label(context)));
            } catch (RuntimeException exception) {
                LabyChatClient.LOGGER.warn("Context menu entry {} failed while opening", apiEntry.getKey(), exception);
            }
        }
        TextRenderer renderer = client.textRenderer;
        width = Math.max(110, entries.stream().mapToInt(entry -> renderer.getWidth(entry.label())).max().orElse(90) + 16);
        open = !entries.isEmpty();
    }

    public void close() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public void render(DrawContext context, TextRenderer renderer, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        if (!open) return;
        int rowHeight = renderer.fontHeight + 8;
        int totalHeight = entries.size() * rowHeight + 4;
        int drawX = Math.min(x, screenWidth - width - 3);
        int drawY = Math.min(y, screenHeight - totalHeight - 3);
        this.x = Math.max(3, drawX);
        this.y = Math.max(3, drawY);
        context.fill(this.x, this.y, this.x + width, this.y + totalHeight, 0xEE111318);
        context.drawStrokedRectangle(this.x, this.y, width, totalHeight, 0xFF5A6575);
        for (int i = 0; i < entries.size(); i++) {
            int rowY = this.y + 2 + i * rowHeight;
            boolean hovered = mouseX >= this.x && mouseX < this.x + width
                    && mouseY >= rowY && mouseY < rowY + rowHeight;
            if (hovered) context.fill(this.x + 2, rowY, this.x + width - 2, rowY + rowHeight, 0xAA334155);
            context.drawTextWithShadow(renderer, entries.get(i).label(), this.x + 8, rowY + 4, 0xFFFFFFFF);
        }
    }

    public boolean mouseClicked(MinecraftClient client, double mouseX, double mouseY, int button) {
        if (!open) return false;
        if (button != 0) {
            close();
            return true;
        }
        int rowHeight = client.textRenderer.fontHeight + 8;
        int totalHeight = entries.size() * rowHeight + 4;
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + totalHeight) {
            close();
            return true;
        }
        int index = (int) ((mouseY - y - 2) / rowHeight);
        if (index >= 0 && index < entries.size()) {
            Entry entry = entries.get(index);
            try {
                entry.value().activate(new ContextMenuContext(client, playerName));
            } catch (RuntimeException exception) {
                LabyChatClient.LOGGER.warn("Context menu entry {} failed", entry.id(), exception);
            }
        }
        close();
        return true;
    }

    private record Entry(String id, IContextMenuEntry value, Text label) {
    }
}
