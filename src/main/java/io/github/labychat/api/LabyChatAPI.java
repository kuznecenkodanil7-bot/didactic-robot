package io.github.labychat.api;

import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LabyChatAPI {
    private static final List<ITransformer> TRANSFORMERS = new CopyOnWriteArrayList<>();
    private static final Map<String, IContextMenuEntry> CONTEXT_ENTRIES = new LinkedHashMap<>();
    private static final Map<String, Identifier> EMOJIS = new LinkedHashMap<>();

    private LabyChatAPI() {
    }

    public static void registerMessageTransformer(ITransformer transformer) {
        TRANSFORMERS.add(transformer);
    }

    public static synchronized void registerContextMenuEntry(String id, IContextMenuEntry entry) {
        CONTEXT_ENTRIES.put(id, entry);
    }

    public static synchronized void addEmoji(String shortcut, Identifier texture) {
        EMOJIS.put(shortcut, texture);
    }

    public static List<ITransformer> transformers() {
        return List.copyOf(TRANSFORMERS);
    }

    public static synchronized Map<String, IContextMenuEntry> contextMenuEntries() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(CONTEXT_ENTRIES));
    }

    public static synchronized Map<String, Identifier> emojis() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(EMOJIS));
    }
}
