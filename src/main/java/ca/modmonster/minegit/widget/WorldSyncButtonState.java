package ca.modmonster.minegit.widget;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class WorldSyncButtonState {
    public static final WorldSyncButtonState SETUP = new WorldSyncButtonState(Component.literal("❗"), Component.translatable("minegit.link.setup"));
    public static final WorldSyncButtonState ENABLE = new WorldSyncButtonState(Component.literal("☁"), Component.translatable("minegit.sync.enable"));
    public static final WorldSyncButtonState WORLD_CONFIGURE = new WorldSyncButtonState(Component.literal("✔"), Component.translatable("minegit.sync.enabled"));

    public Component message;
    public Tooltip tooltip;

    public WorldSyncButtonState(Component message, Component tooltip) {
        this.message = message;
        this.tooltip = Tooltip.create(tooltip);
    }

    public void apply(Button button) {
        button.setMessage(message);
        button.setTooltip(tooltip);
    }
}
