package io.github.labychat.api;

import net.minecraft.text.Text;

import java.util.Optional;

@FunctionalInterface
public interface ITransformer {
    /**
     * Transforms a display-only copy of a received message.
     * Returning Optional.empty() hides it only in LabyChat's renderer.
     */
    Optional<Text> transform(Text message, TransformContext context);
}
