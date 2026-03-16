package ca.modmonster.minegit.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;

public class WorldSyncScreen extends Screen {
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 8 + 9 + 8 + 20 + 4, 60);

    private final Screen parent;
    private final LevelSummary level;
    private final Runnable closeCallback;

    public WorldSyncScreen(Screen parent, LevelSummary level) {
        this(parent, level, null);
    }

    public WorldSyncScreen(Screen parent, LevelSummary level, Runnable closeCallback) {
        super(Component.translatable("minegit.sync.title"));
        this.parent = parent;
        this.level = level;
        this.closeCallback = closeCallback;
    }

    @Override
    protected void init() {
        // Column layout
        LinearLayout columnLayout = this.layout.addToContents(LinearLayout.vertical().spacing(8));
        columnLayout.defaultCellSetting().alignHorizontallyCenter();

        // Menu title
        layout.addTitleHeader(this.title, this.font);

        // Level name
        columnLayout.addChild(new StringWidget(Component.literal(level.getLevelName()), this.font));

        // Add layout widgets
        this.layout.visitWidgets(this::addRenderableWidget);
        this.layout.arrangeElements();

        // Back button
        Button backButton = Button.builder(Component.literal("←"), button -> onClose())
            .tooltip(Tooltip.create(Component.translatable("minegit.sync.back")))
            .bounds(6, 6, 20, 20)
            .build();
        addRenderableWidget(backButton);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
        if (closeCallback != null) closeCallback.run();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
