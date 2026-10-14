package ca.modmonster.minegit.mixin;

import ca.modmonster.minegit.data.ConfigManager;
import ca.modmonster.minegit.data.GitManager;
import ca.modmonster.minegit.data.QuitState;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.CommonColors;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private Button disconnectButton;
    @Unique
    private final WidgetTooltipHolder tooltipHolder = new WidgetTooltipHolder();

    @Unique
    private final Tooltip tooltip = Tooltip.create(Component.translatable("minegit.exit_without_push", ConfigManager.getCurrentConfig().gitService.getNaturalName()));

    @Inject(at = @At("TAIL"), method = "createPauseMenu")
    private void createPauseMenu(CallbackInfo ci) {
        // Find the disconnect button
        for (AbstractWidget widget : Screens.getWidgets(this)) {
            if (!(widget instanceof Button button)) continue;
            Component buttonMessage = button.getMessage();
            if (!(buttonMessage.getContents() instanceof TranslatableContents contents)) continue;
            if (contents.getKey().equals("menu.returnToMenu")) {
                disconnectButton = button;
                break;
            }
        }

        tooltipHolder.set(tooltip);
    }

    @Inject(at = @At("TAIL"), method = "extractRenderState", remap = false)
    private void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a, CallbackInfo info) {
        if (disconnectButton == null) return;
        if (!minecraft.isLocalServer()) return;
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null) return;
        if (!GitManager.syncEnabled(server.getWorldPath(LevelResource.ROOT))) return;
        if (!QuitState.altQuit) return;

        // Render tooltip
        tooltipHolder.refreshTooltipForNextRenderPass(graphics, mouseX, mouseY, disconnectButton.isHovered(), disconnectButton.isFocused(), disconnectButton.getRectangle());

        if (disconnectButton.isHoveredOrFocused()) {
            // draw red border
            graphics.outline(
                    disconnectButton.getX(),
                    disconnectButton.getY(),
                    disconnectButton.getWidth(),
                    disconnectButton.getHeight(),
                    CommonColors.RED
            );
        }
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (event.key() == InputConstants.KEY_LALT) QuitState.altQuit = true;
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (event.key() == InputConstants.KEY_LALT) QuitState.altQuit = false;
        return super.keyReleased(event);
    }
}
