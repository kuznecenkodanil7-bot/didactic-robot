package io.github.labychat.screen;

import io.github.labychat.LabyChatClient;
import io.github.labychat.chat.EmojiRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public final class EmojiScreen extends Screen {
    private final Screen parent;
    private List<EmojiRegistry.EmojiChoice> choices = List.of();
    private String status = "";

    public EmojiScreen(Screen parent) {
        super(Text.translatable("screen.labychat.emoji"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        choices = LabyChatClient.emojis().choices();
        int columns = Math.max(1, Math.min(6, (width - 30) / 105));
        int startX = width / 2 - (columns * 100) / 2;
        int startY = 42;
        for (int i = 0; i < choices.size() && i < 36; i++) {
            EmojiRegistry.EmojiChoice choice = choices.get(i);
            int column = i % columns;
            int row = i / columns;
            String label = choice.insertion() + "  " + choice.shortcut();
            addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> copy(choice))
                    .dimensions(startX + column * 100, startY + row * 24, 96, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(width / 2 - 75, height - 28, 150, 20).build());
    }

    private void copy(EmojiRegistry.EmojiChoice choice) {
        client.keyboard.setClipboard(choice.shortcut());
        status = Text.translatable("screen.labychat.copied", choice.shortcut()).getString();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFFFF);
        if (!status.isBlank()) context.drawCenteredTextWithShadow(textRenderer, status, width / 2, height - 45, 0xFF86EFAC);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
