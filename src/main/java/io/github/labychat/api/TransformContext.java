package io.github.labychat.api;

import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public record TransformContext(
        MinecraftClient client,
        Instant receivedAt,
        @Nullable String parsedSender,
        boolean systemMessage
) {
}
