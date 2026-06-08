package io.github.labychat.screen;

import io.github.labychat.LabyChatClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class ChatLogScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget search;
    private List<String> lines = new ArrayList<>();
    private int offset;

    public ChatLogScreen(Screen parent) {
        super(Text.translatable("screen.labychat.log"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        search = new TextFieldWidget(textRenderer, width / 2 - 150, 34, 300, 20,
                Text.translatable("screen.labychat.search"));
        search.setPlaceholder(Text.translatable("screen.labychat.search"));
        search.setMaxLength(128);
        search.setChangedListener(value -> refresh());
        addDrawableChild(search);
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(width / 2 - 75, height - 28, 150, 20).build());
        setInitialFocus(search);
        refresh();
    }

    private void refresh() {
        String query = search == null ? "" : search.getText();
        List<String> memory = LabyChatClient.chat().searchMemory(query, 400);
        List<String> disk = LabyChatClient.chat().logWriter().search(query, 400);
        lines = new ArrayList<>(memory);
        for (String line : disk) {
            if (!lines.contains(line)) lines.add(line);
            if (lines.size() >= 500) break;
        }
        offset = 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFFFF);
        int top = 62;
        int bottom = height - 36;
        context.fill(12, top - 5, width - 12, bottom, 0x99101418);
        context.enableScissor(14, top, width - 14, bottom - 2);
        int lineHeight = textRenderer.fontHeight + 3;
        int maxVisible = Math.max(1, (bottom - top) / lineHeight);
        int end = Math.min(lines.size(), offset + maxVisible);
        int y = top;
        for (int i = offset; i < end; i++) {
            String trimmed = textRenderer.trimToWidth(lines.get(i), width - 36);
            context.drawTextWithShadow(textRenderer, trimmed, 18, y, 0xFFE5E7EB);
            y += lineHeight;
        }
        context.disableScissor();
        context.drawTextWithShadow(textRenderer,
                Text.literal(lines.size() + " results"), 16, height - 25, 0xFF9CA3AF);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!lines.isEmpty()) {
            offset = Math.max(0, Math.min(Math.max(0, lines.size() - 1), offset + (verticalAmount < 0 ? 3 : -3)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
