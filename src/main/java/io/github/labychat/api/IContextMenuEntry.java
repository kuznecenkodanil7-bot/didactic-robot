package io.github.labychat.api;

import net.minecraft.text.Text;

public interface IContextMenuEntry {
    Text label(ContextMenuContext context);

    default boolean isVisible(ContextMenuContext context) {
        return true;
    }

    void activate(ContextMenuContext context);
}
