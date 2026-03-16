package ca.modmonster.minegit.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ca.modmonster.minegit.data.Config;
import ca.modmonster.minegit.data.ConfigManager;
import ca.modmonster.minegit.gui.AccountLinkScreen;
import ca.modmonster.minegit.gui.EnableWorldSyncScreen;
import ca.modmonster.minegit.gui.WorldSyncScreen;
import ca.modmonster.minegit.widget.WorldSyncButtonState;

@Mixin(SelectWorldScreen.class)
public class SinglePlayerScreenMixin extends Screen {
    @Shadow
    private @Nullable WorldSelectionList list;

    protected SinglePlayerScreenMixin(Component title) {
        super(title);
    }

    @Unique @Nullable
    private Button worldSyncButton;

    @Unique
    private WorldSyncButtonState worldSyncButtonState = WorldSyncButtonState.SETUP;

    @Unique @Nullable
    private LevelSummary hoveredLevel;

    @Inject(at = @At("TAIL"), method = "init", remap = false)
	private void init(CallbackInfo info) {
        // Add world sync button
        worldSyncButton = Button.builder(Component.literal("☁"), button -> {
            if (worldSyncButtonState == WorldSyncButtonState.SETUP) {
                this.minecraft.setScreen(new AccountLinkScreen(this, () -> {
                    if (this.list != null) this.list.returnToScreen();
                }));
            } else if (worldSyncButtonState == WorldSyncButtonState.ENABLE) {
                if (hoveredLevel != null) this.minecraft.setScreen(new EnableWorldSyncScreen(this, hoveredLevel, () -> {
                    if (this.list != null) this.list.returnToScreen();
                }));
            } else if (worldSyncButtonState == WorldSyncButtonState.WORLD_CONFIGURE) {
                if (hoveredLevel != null) this.minecraft.setScreen(new WorldSyncScreen(this, hoveredLevel, () -> {
                    if (this.list != null) this.list.returnToScreen();
                }));
            }
        }).size(20, 20).build();
        worldSyncButton.active = false;
        addRenderableWidget(worldSyncButton);

        repositionElements();
        updateWorldSyncButton();
	}

    @Inject(at = @At("TAIL"), method = "updateButtonStatus", remap = false)
    private void updateButtonStatus(LevelSummary levelSummary, CallbackInfo ci) {
        if (worldSyncButton == null) return;
        if (levelSummary != null) hoveredLevel = levelSummary;
        this.worldSyncButton.active = levelSummary != null || worldSyncButtonState == WorldSyncButtonState.SETUP;
    }

    @Inject(at = @At("TAIL"), method = "repositionElements", remap = false)
    protected void repositionElements(CallbackInfo ci) {
        if (worldSyncButton != null) worldSyncButton.setPosition(width / 2 - 178, height - 52);
    }

    @Unique
    private void updateWorldSyncButton() {
        if (worldSyncButton == null) return;
        Config config = ConfigManager.getCurrentConfig();
        if (config.username.isBlank() || config.getPat().isBlank()) {
            // Set the world sync button to configuration state
            worldSyncButtonState = WorldSyncButtonState.SETUP;
        } else {
            // TODO: check if syncing already enabled for world
            worldSyncButtonState = WorldSyncButtonState.ENABLE;
        }

        worldSyncButtonState.apply(worldSyncButton);
    }
}