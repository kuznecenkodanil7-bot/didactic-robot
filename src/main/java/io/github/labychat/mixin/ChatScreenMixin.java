package io.github.labychat.mixin;

import io.github.labychat.LabyChatClient;
import io.github.labychat.screen.ChatLogScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Style;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Invoker("handleClickEvent")
    protected abstract boolean labychat$handleClickEvent(Style style, boolean insert);

    @ModifyVariable(method = "sendMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String labychat$replaceOutgoingEmoji(String message) {
        return LabyChatClient.chat().transformOutgoing(message);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void labychat$mouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (LabyChatClient.renderer().contextMenu().mouseClicked(client, click.x(), click.y(), click.button())) {
            cir.setReturnValue(true);
            return;
        }
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            String sender = LabyChatClient.renderer().senderAt(click.x(), click.y());
            if (sender != null) {
                LabyChatClient.renderer().contextMenu().open(client, sender, (int) click.x(), (int) click.y());
                cir.setReturnValue(true);
                return;
            }
        }
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Style style = LabyChatClient.renderer().styleAt(click.x(), click.y(), client.textRenderer);
            if (style != null && style.getClickEvent() != null && labychat$handleClickEvent(style, click.hasShift())) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void labychat$mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                        double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (LabyChatClient.renderer().scroll(verticalAmount)) cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void labychat$keyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (input.key() == GLFW.GLFW_KEY_F && input.hasCtrlOrCmd()) {
            MinecraftClient client = MinecraftClient.getInstance();
            client.setScreen(new ChatLogScreen((ChatScreen) (Object) this));
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void labychat$renderOverlay(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        Style style = LabyChatClient.renderer().styleAt(mouseX, mouseY, client.textRenderer);
        if (style != null) context.drawHoverEvent(client.textRenderer, style, mouseX, mouseY);
        LabyChatClient.renderer().contextMenu().render(context, client.textRenderer,
                context.getScaledWindowWidth(), context.getScaledWindowHeight(), mouseX, mouseY);
    }
}
