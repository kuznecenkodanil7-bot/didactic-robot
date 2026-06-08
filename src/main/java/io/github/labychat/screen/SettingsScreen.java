package io.github.labychat.screen;

import io.github.labychat.LabyChatClient;
import io.github.labychat.config.LabyChatConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private final List<String> themeIds = new ArrayList<>();

    public SettingsScreen(Screen parent) {
        super(Text.translatable("screen.labychat.settings"));
        this.parent = parent;
        themeIds.addAll(LabyChatClient.themes().all().keySet());
    }

    @Override
    protected void init() {
        int left = width / 2 - 155;
        int right = width / 2 + 5;
        int y = 52;
        addDrawableChild(button(left, y, labelEnabled(), button -> {
            config().enabled = !config().enabled;
            button.setMessage(labelEnabled());
            save();
        }));
        addDrawableChild(button(right, y, labelTheme(), button -> {
            cycleTheme();
            button.setMessage(labelTheme());
        }));
        y += 25;
        addDrawableChild(button(left, y, labelOpacity(), button -> {
            config().backgroundOpacity = config().backgroundOpacity >= 1.0f ? 0.0f : config().backgroundOpacity + 0.1f;
            button.setMessage(labelOpacity());
            save();
        }));
        addDrawableChild(button(right, y, labelWidth(), button -> {
            config().width = config().width >= 640 ? 100 : config().width + 40;
            button.setMessage(labelWidth());
            save();
        }));
        y += 25;
        addDrawableChild(button(left, y, labelAnimation(), button -> {
            LabyChatConfig.Animation[] values = LabyChatConfig.Animation.values();
            config().animation = values[(config().animation.ordinal() + 1) % values.length];
            button.setMessage(labelAnimation());
            save();
        }));
        addDrawableChild(button(right, y, labelAvatars(), button -> {
            config().showAvatars = !config().showAvatars;
            button.setMessage(labelAvatars());
            save();
        }));
        y += 25;
        addDrawableChild(button(left, y, labelFont(), button -> {
            config().fontScale = config().fontScale >= 2.0f ? 0.5f : config().fontScale + 0.25f;
            button.setMessage(labelFont());
            save();
        }));
        addDrawableChild(button(right, y, labelDelay(), button -> {
            config().disappearDelaySeconds = config().disappearDelaySeconds >= 60 ? 1 : config().disappearDelaySeconds + 5;
            button.setMessage(labelDelay());
            save();
        }));
        y += 31;
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.labychat.open_log"),
                button -> client.setScreen(new ChatLogScreen(this)))
                .dimensions(left, y, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.labychat.open_emoji"),
                button -> client.setScreen(new EmojiScreen(this)))
                .dimensions(right, y, 150, 20).build());
        y += 25;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(width / 2 - 75, y, 150, 20).build());
    }

    private ButtonWidget button(int x, int y, Text label, ButtonWidget.PressAction action) {
        return ButtonWidget.builder(label, action).dimensions(x, y, 150, 20).build();
    }

    private void cycleTheme() {
        if (themeIds.isEmpty()) return;
        int current = themeIds.indexOf(config().theme);
        int next = (current + 1 + themeIds.size()) % themeIds.size();
        LabyChatClient.themes().apply(themeIds.get(next));
    }

    private LabyChatConfig config() {
        return LabyChatClient.config().get();
    }

    private void save() {
        LabyChatClient.config().save();
    }

    private Text labelEnabled() {
        return Text.literal("Chat: " + onOff(config().enabled));
    }

    private Text labelTheme() {
        return Text.literal("Theme: " + config().theme);
    }

    private Text labelOpacity() {
        return Text.literal("Opacity: " + Math.round(config().backgroundOpacity * 100) + "%");
    }

    private Text labelWidth() {
        return Text.literal("Width: " + config().width + " px");
    }

    private Text labelAnimation() {
        return Text.literal("Animation: " + config().animation.name().toLowerCase());
    }

    private Text labelAvatars() {
        return Text.literal("Avatars: " + onOff(config().showAvatars));
    }

    private Text labelFont() {
        return Text.literal("Font: " + String.format(java.util.Locale.ROOT, "%.2fx", config().fontScale));
    }

    private Text labelDelay() {
        return Text.literal("Fade delay: " + config().disappearDelaySeconds + " s");
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xFFFFFFFF);
        int previewWidth = Math.min(620, width - 30);
        int previewHeight = Math.max(72, height - 245);
        LabyChatClient.renderer().renderPreview(context, textRenderer, width / 2 - previewWidth / 2,
                height - previewHeight - 18, previewWidth, previewHeight);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void close() {
        save();
        client.setScreen(parent);
    }
}
