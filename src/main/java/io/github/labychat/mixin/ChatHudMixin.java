package io.github.labychat.mixin;

import io.github.labychat.LabyChatClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"))
    private void labychat$capture(Text message, @Nullable MessageSignatureData signatureData,
                                  @Nullable MessageIndicator indicator, CallbackInfo ci) {
        LabyChatClient.chat().capture(message, signatureData);
        LabyChatClient.renderer().resetScroll();
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("HEAD"), cancellable = true)
    private void labychat$render(DrawContext context, TextRenderer textRenderer, int currentTick,
                                 int mouseX, int mouseY, boolean interactable, boolean bool, CallbackInfo ci) {
        if (!LabyChatClient.config().get().enabled) return;
        LabyChatClient.renderer().render(context, textRenderer, mouseX, mouseY, interactable);
        ci.cancel();
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void labychat$clear(boolean clearHistory, CallbackInfo ci) {
        LabyChatClient.chat().clear();
        LabyChatClient.renderer().resetScroll();
    }

    @Inject(method = "removeMessage", at = @At("HEAD"))
    private void labychat$remove(MessageSignatureData signature, CallbackInfo ci) {
        LabyChatClient.chat().removeBySignature(signature);
    }
}
