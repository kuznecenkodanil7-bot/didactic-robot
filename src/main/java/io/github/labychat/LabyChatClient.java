package io.github.labychat;

import io.github.labychat.api.ContextMenuContext;
import io.github.labychat.api.IContextMenuEntry;
import io.github.labychat.api.LabyChatAPI;
import io.github.labychat.chat.ChatLogWriter;
import io.github.labychat.chat.ChatManager;
import io.github.labychat.chat.EmojiRegistry;
import io.github.labychat.compat.PrivateMessageCommandDetector;
import io.github.labychat.config.ConfigManager;
import io.github.labychat.config.ThemeManager;
import io.github.labychat.render.ChatRenderer;
import io.github.labychat.screen.ChatLogScreen;
import io.github.labychat.screen.EmojiScreen;
import io.github.labychat.screen.SettingsScreen;
import io.github.labychat.translation.LibreTranslateClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class LabyChatClient implements ClientModInitializer {
    public static final String MOD_ID = "labychat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ConfigManager config;
    private static ThemeManager themes;
    private static EmojiRegistry emojis;
    private static ChatLogWriter logWriter;
    private static ChatManager chat;
    private static ChatRenderer renderer;
    private static LibreTranslateClient translator;

    @Override
    public void onInitializeClient() {
        config = new ConfigManager();
        config.load();
        themes = new ThemeManager(config);
        themes.load();
        emojis = new EmojiRegistry(config);
        emojis.scanCustom();
        logWriter = new ChatLogWriter(config);
        chat = new ChatManager(config, emojis, logWriter);
        renderer = new ChatRenderer(chat, config);
        translator = new LibreTranslateClient(config);

        registerDefaultContextEntries();
        registerKeys();
        registerClientCommands();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> logWriter.close());
        LOGGER.info("LabyChat Standalone initialized in client-only mode");
    }

    private static void registerKeys() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        KeyBinding settings = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.labychat.settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, category));
        KeyBinding emoji = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.labychat.emoji", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F7, category));
        KeyBinding quickReply = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.labychat.quick_reply", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F4, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (settings.wasPressed()) client.setScreen(new SettingsScreen(client.currentScreen));
            while (emoji.wasPressed()) client.setScreen(new EmojiScreen(client.currentScreen));
            while (quickReply.wasPressed()) sendQuickReply(client);
        });
    }

    private static void sendQuickReply(MinecraftClient client) {
        if (!config.get().outgoingQuickRepliesEnabled || client.getNetworkHandler() == null) return;
        String text = config.get().quickReplies.getOrDefault("F4", "Я в AFK, скоро вернусь");
        client.getNetworkHandler().sendChatMessage(chat.transformOutgoing(text));
    }

    private static void registerClientCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("labychat")
                        .then(literal("config").executes(context -> {
                            MinecraftClient.getInstance().setScreen(new SettingsScreen(MinecraftClient.getInstance().currentScreen));
                            return 1;
                        }))
                        .then(literal("log").executes(context -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            client.setScreen(new ChatLogScreen(client.currentScreen));
                            return 1;
                        }))
                        .then(literal("emoji").executes(context -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            client.setScreen(new EmojiScreen(client.currentScreen));
                            return 1;
                        }))
        ));
    }

    private static void registerDefaultContextEntries() {
        LabyChatAPI.registerContextMenuEntry("private_message", new IContextMenuEntry() {
            @Override
            public Text label(ContextMenuContext context) {
                return Text.translatable("menu.labychat.private_message");
            }

            @Override
            public void activate(ContextMenuContext context) {
                String command = PrivateMessageCommandDetector.command(context.client(), config.get());
                context.client().setScreen(new ChatScreen(command + " " + context.playerName() + " ", false));
            }
        });
        LabyChatAPI.registerContextMenuEntry("profile", new IContextMenuEntry() {
            @Override
            public Text label(ContextMenuContext context) {
                return Text.translatable("menu.labychat.profile");
            }

            @Override
            public void activate(ContextMenuContext context) {
                Util.getOperatingSystem().open(URI.create("https://namemc.com/profile/" + context.playerName()));
            }
        });
        LabyChatAPI.registerContextMenuEntry("copy_name", new IContextMenuEntry() {
            @Override
            public Text label(ContextMenuContext context) {
                return Text.translatable("menu.labychat.copy_name");
            }

            @Override
            public void activate(ContextMenuContext context) {
                context.client().keyboard.setClipboard(context.playerName());
            }
        });
    }

    public static ConfigManager config() {
        return config;
    }

    public static ThemeManager themes() {
        return themes;
    }

    public static EmojiRegistry emojis() {
        return emojis;
    }

    public static ChatManager chat() {
        return chat;
    }

    public static ChatRenderer renderer() {
        return renderer;
    }

    public static LibreTranslateClient translator() {
        return translator;
    }
}
