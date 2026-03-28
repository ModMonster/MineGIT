package ca.modmonster.minegit.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import org.eclipse.jgit.lib.ProgressMonitor;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public class GitProgressScreen extends Screen implements ProgressMonitor {
    public static final int PROGRESS_BAR_WIDTH = 128;

    @Nullable
    private FocusableTextWidget textWidget;

    @Nullable
    private StringWidget currentTaskWidget;

    private int currentTaskWork = 0;
    private int currentTaskTotalWork = 1;

    public GitProgressScreen(Component component) {
        super(component);
    }

    @Override
    protected void init() {
        this.textWidget = this.addRenderableWidget(FocusableTextWidget.builder(this.title, this.font, 12).textWidth(this.font.width(this.title)).build());
        this.currentTaskWidget = this.addRenderableWidget(new StringWidget(Component.empty(), font));
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        if (this.textWidget != null) {
            this.textWidget.setPosition(this.width / 2 - this.textWidget.getWidth() / 2, this.height / 2 - 9 / 2);
        }

        if (this.currentTaskWidget != null) {
            this.currentTaskWidget.setPosition(this.width / 2 - this.currentTaskWidget.getWidth() / 2, this.height - 32);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected boolean shouldNarrateNavigation() {
        return false;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderPanorama(guiGraphics, f);
        this.renderBlurredBackground(guiGraphics);
        this.renderMenuBackground(guiGraphics);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);

        // Render progress bar
        int barLeft = this.width / 2 - PROGRESS_BAR_WIDTH / 2;
        guiGraphics.fill(barLeft, this.height - 16, barLeft + PROGRESS_BAR_WIDTH, this.height - 18, 0xFFA0A0A0);

        float progress = (float) currentTaskWork / currentTaskTotalWork;
        int barPixels = (int) (PROGRESS_BAR_WIDTH * progress);
        guiGraphics.fill(barLeft, this.height - 16, barLeft + barPixels, this.height - 18, 0xFF80FF80);
    }

    @Override
    public void start(int totalTasks) {}

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void beginTask(String title, int totalWork) {
        if (currentTaskWidget != null) {
            minecraft.submit(() -> {
                currentTaskWidget.setMessage(Component.literal(title));
                repositionElements();
            });
        }
        currentTaskWork = 0;
        currentTaskTotalWork = totalWork != 0? totalWork : 1;
    }

    @Override
    public void update(int completed) {
        currentTaskWork += completed;
    }

    @Override
    public void endTask() {}

    @Override
    public boolean isCancelled() {return false;}

    @Override
    public void showDuration(boolean enabled) {}
}
