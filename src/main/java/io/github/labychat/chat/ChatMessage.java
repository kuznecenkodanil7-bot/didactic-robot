package io.github.labychat.chat;

import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public final class ChatMessage {
    private final UUID id = UUID.randomUUID();
    private final Text original;
    private Text display;
    private final @Nullable String sender;
    private final int senderPrefixCodePoints;
    private final Instant receivedAt;
    private final long receivedNanos;
    private final @Nullable MessageSignatureData signature;
    private final boolean mention;
    private int duplicateCount;

    public ChatMessage(Text original, Text display, @Nullable String sender, int senderPrefixCodePoints,
                       Instant receivedAt, long receivedNanos, @Nullable MessageSignatureData signature,
                       boolean mention) {
        this.original = original;
        this.display = display;
        this.sender = sender;
        this.senderPrefixCodePoints = senderPrefixCodePoints;
        this.receivedAt = receivedAt;
        this.receivedNanos = receivedNanos;
        this.signature = signature;
        this.mention = mention;
        this.duplicateCount = 1;
    }

    public UUID id() { return id; }
    public Text original() { return original; }
    public Text display() { return display; }
    public void setDisplay(Text display) { this.display = display; }
    public @Nullable String sender() { return sender; }
    public int senderPrefixCodePoints() { return senderPrefixCodePoints; }
    public Instant receivedAt() { return receivedAt; }
    public long receivedNanos() { return receivedNanos; }
    public @Nullable MessageSignatureData signature() { return signature; }
    public boolean mention() { return mention; }
    public int duplicateCount() { return duplicateCount; }
    public void incrementDuplicateCount() { duplicateCount++; }
}
