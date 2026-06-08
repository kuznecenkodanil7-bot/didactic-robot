package io.github.labychat.compat;

import io.github.labychat.config.LabyChatConfig;
import net.minecraft.client.MinecraftClient;

import java.util.Locale;

public final class PrivateMessageCommandDetector {
    private static final String[] PASSIVE_CANDIDATES = {"msg", "tell", "w", "whisper", "message"};

    private PrivateMessageCommandDetector() {
    }

    /**
     * Passive detection only. The server already supplies its Brigadier command tree to the
     * vanilla client, so inspecting it does not send probes or custom packets. If no familiar
     * private-message command is advertised, a small network-name fallback is used and then
     * /msg remains the conservative default.
     */
    public static String command(MinecraftClient client, LabyChatConfig config) {
        if (!"auto".equalsIgnoreCase(config.privateMessageCommand)) {
            return normalize(config.privateMessageCommand);
        }
        if (client.getNetworkHandler() != null) {
            var root = client.getNetworkHandler().getCommandDispatcher().getRoot();
            for (String candidate : PASSIVE_CANDIDATES) {
                if (root.getChild(candidate) != null) return "/" + candidate;
            }
        }
        if (client.getCurrentServerEntry() != null) {
            String address = client.getCurrentServerEntry().address.toLowerCase(Locale.ROOT);
            if (address.contains("hypixel")) return "/msg";
            if (address.contains("gommehd") || address.contains("timolia")) return "/tell";
        }
        return "/msg";
    }

    private static String normalize(String command) {
        String value = command.trim();
        if (value.isEmpty()) return "/msg";
        return value.startsWith("/") ? value : "/" + value;
    }
}
