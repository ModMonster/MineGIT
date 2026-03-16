package ca.modmonster.minegit.gui;

import com.google.gson.JsonParser;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;

import java.net.http.HttpResponse;

import ca.modmonster.minegit.MineGIT;
import ca.modmonster.minegit.data.Config;
import ca.modmonster.minegit.data.ConfigManager;
import ca.modmonster.minegit.data.GitManager;
import ca.modmonster.minegit.data.NetworkManager;

public class EnableWorldSyncScreen extends Screen {
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 8 + 9 + 8 + 20 + 4, 60);

    private final Screen parent;
    private final LevelSummary level;
    private final Runnable closeCallback;

    private Button confirmButton;
    private Button cancelButton;
    private Button openSetupButton;
    private StringWidget statusWidget;

    public EnableWorldSyncScreen(Screen parent, LevelSummary level) {
        this(parent, level, null);
    }

    public EnableWorldSyncScreen(Screen parent, LevelSummary level, Runnable closeCallback) {
        super(Component.translatable("minegit.sync.enable"));
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

        // Confirmation message
        columnLayout.addChild(new StringWidget(Component.translatable("minegit.sync.enable.confirm.line1", level.getLevelName()), this.font));
        columnLayout.addChild(new StringWidget(Component.translatable("minegit.sync.enable.confirm.line2"), this.font));

        // Confirm button
        LinearLayout buttonRowLayout = columnLayout.addChild(LinearLayout.horizontal().spacing(8));
        confirmButton = Button.builder(Component.translatable("minegit.sync.enable.confirm.ok"), button -> setupSync()).build();
        buttonRowLayout.addChild(confirmButton);

        // Cancel button
        cancelButton = Button.builder(Component.translatable("minegit.sync.enable.confirm.cancel"), button -> onClose()).build();
        buttonRowLayout.addChild(cancelButton);

        // Status
        statusWidget = new StringWidget(Component.empty(), font);
        columnLayout.addChild(statusWidget);

        openSetupButton = Button.builder(Component.translatable("minegit.link.setup.open"), button -> minecraft.setScreen(new AccountLinkScreen(this.parent))).build();
        openSetupButton.visible = false;
        columnLayout.addChild(openSetupButton);

        // Add layout widgets
        this.layout.visitWidgets(this::addRenderableWidget);
        this.layout.arrangeElements();
    }

    private void setupSync() {
        confirmButton.active = false;
        cancelButton.active = false;

        // Create a repository on GitHub
        Config config = ConfigManager.getCurrentConfig();
        statusWidget.setMessage(Component.translatable("minegit.sync.enable.status.create_repo"));
        repositionElements();
        HttpResponse<String> response = NetworkManager.createRepo(config.getPat(), level.getLevelId(), level.getLevelName());
        int statusCode = response == null? -1 : response.statusCode();
        if (statusCode != 201) {
            // OOPS! ERROR!!
            statusWidget.setMessage(Component.translatable("minegit.sync.enable.status.create_repo.error", statusCode));
            openSetupButton.visible = true;
            cancelButton.active = true;
            repositionElements();

            if (response != null) MineGIT.LOGGER.error(response.body());
            return;
        }

        String repoUrl = JsonParser.parseString(response.body()).getAsJsonObject().get("clone_url").getAsString();
        MineGIT.LOGGER.info("Successfully setup GitHub repo with URL: {}", repoUrl);

        // Git init on world save folder
        statusWidget.setMessage(Component.translatable("minegit.sync.status.git_init"));
        repositionElements();
        boolean ok = GitManager.init(minecraft, level.getLevelId(), repoUrl);
        if (!ok) {
            statusWidget.setMessage(Component.translatable("minegit.sync.status.git_init_error"));
            repositionElements();
            return;
        }

        minecraft.getToastManager().addToast(new SystemToast(new SystemToast.SystemToastId(), Component.translatable("minegit.sync.enable.complete"), null));
        onClose();
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
        return cancelButton.active;
    }
}
