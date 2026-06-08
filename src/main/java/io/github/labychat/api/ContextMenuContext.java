package io.github.labychat.api;

import net.minecraft.client.MinecraftClient;

public record ContextMenuContext(MinecraftClient client, String playerName) {
}
