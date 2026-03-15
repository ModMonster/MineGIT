package ca.modmonster.minegit.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ca.modmonster.minegit.gui.AccountLinkScreen;

@Mixin(SelectWorldScreen.class)
public class SinglePlayerScreenMixin extends Screen {
    protected SinglePlayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(at = @At("TAIL"), method = "init", remap = false)
	private void init(CallbackInfo info) {
        addRenderableWidget(
            Button.builder(Component.literal("☁"), button -> this.minecraft.setScreen(new AccountLinkScreen(this)))
                .bounds(6, 6, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("minegit.link.title")))
                .build()
        );
	}
}