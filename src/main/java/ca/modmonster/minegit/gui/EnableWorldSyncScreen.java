package ca.modmonster.minegit.gui;

import ca.modmonster.minegit.MineGIT;
import ca.modmonster.minegit.data.Config;
import ca.modmonster.minegit.data.ConfigManager;
import ca.modmonster.minegit.data.GitManager;
import ca.modmonster.minegit.data.NetworkManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;

import java.net.http.HttpResponse;

public class EnableWorldSyncScreen extends Screen {
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 8 + 9 + 8 + 20 + 4, 60);

    private final Screen parent;
    private final LevelSummary level;
    private final Runnable closeCallback;

    private Button confirmButton;
    private Button cancelButton;
    private Button openSetupButton;
    private EditBox repoUrlEdit;

    public EnableWorldSyncScreen(Screen parent, LevelSummary level, Runnable closeCallback) {
        super(Component.translatable("minegit.sync.enable.title"));
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

        Config config = ConfigManager.getCurrentConfig();

        // Determine if we need manual repo URL input
        boolean needsManualRepoUrl = config.gitService.requiresCustomUrl() &&
                                      !config.gitService.supportsAutoRepoCreation();

        if (needsManualRepoUrl) {
            // Custom service without auto-repo support - ask for repo URL
            columnLayout.addChild(new StringWidget(Component.translatable("minegit.sync.enable.confirm.custom_service.line1", level.getLevelName()), this.font));
            columnLayout.addChild(new StringWidget(Component.translatable("minegit.sync.enable.confirm.custom_service.line2"), this.font));

            StringWidget repoUrlLabel = columnLayout.addChild(new StringWidget(
                    Component.translatable("minegit.sync.enable.repo_url"), this.font));
            repoUrlLabel.setAlpha(0.5f);

            repoUrlEdit = new EditBox(font, 0, 0, 200, 20, Component.translatable("minegit.sync.enable.repo_url"));
            repoUrlEdit.setMaxLength(255);
            columnLayout.addChild(repoUrlEdit);
        } else {
            // GitHub/GitLab/Gitea - show confirmation and auto-create
            columnLayout.addChild(new StringWidget(Component.translatable("minegit.sync.enable.confirm.line1", level.getLevelName()), this.font));
            columnLayout.addChild(new StringWidget(Component.translatable("minegit.sync.enable.confirm.line2", config.gitService.getNaturalName()), this.font));
        }

        // Confirm button
        LinearLayout buttonRowLayout = columnLayout.addChild(LinearLayout.horizontal().spacing(8));
        confirmButton = Button.builder(Component.translatable("minegit.sync.enable.confirm.ok"), _ -> setupSync()).build();
        buttonRowLayout.addChild(confirmButton);

        // Cancel button
        cancelButton = Button.builder(Component.translatable("gui.cancel"), _ -> onClose()).build();
        buttonRowLayout.addChild(cancelButton);

        openSetupButton = Button.builder(Component.translatable("minegit.link.setup.open"), _ -> minecraft.gui.setScreen(new AccountLinkScreen(this.parent, closeCallback))).build();
        openSetupButton.setVisible(false);
        columnLayout.addChild(openSetupButton);

        // Add layout widgets
        this.layout.visitWidgets(this::addRenderableWidget);
        this.layout.arrangeElements();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void setupSync() {
        confirmButton.active = false;
        cancelButton.active = false;

        GitProgressScreen progressScreen = new GitProgressScreen(Component.translatable("minegit.sync.enable.working"));
        minecraft.gui.setScreen(progressScreen);

        Config config = ConfigManager.getCurrentConfig();

        new Thread(() -> {
            String repoUrl;

            // Check if service supports auto-repo creation
            if (config.gitService.supportsAutoRepoCreation()) {
                // Try to create repo automatically (GitHub, GitLab, Gitea)
                progressScreen.beginTask("Create repository", 0);
                HttpResponse<String> response = NetworkManager.createRepo(config, level.getLevelId(), level.getLevelName());
                int statusCode = response == null ? -1 : response.statusCode();

                boolean createSuccess = (statusCode == 201);

                if (!createSuccess) {
                    minecraft.submit(() -> {
                        SystemToast.add(minecraft.gui.toastManager(), new SystemToast.SystemToastId(),
                                Component.translatable("minegit.sync.enable.create_repo.error", config.gitService.getNaturalName(), statusCode), null);
                        openSetupButton.setVisible(true);
                        cancelButton.active = true;
                        if (response != null) MineGIT.LOGGER.error(response.body());
                        minecraft.gui.setScreen(this);
                    });
                    return;
                }

                repoUrl = NetworkManager.parseCloneUrl(response, config.gitService);
                if (repoUrl == null) {
                    // Fallback: construct URL from config
                    repoUrl = config.buildCloneUrl("minegit_" + level.getLevelId());
                }
                MineGIT.LOGGER.info("Successfully created repo with URL: {}", repoUrl);
            } else {
                // Custom service without auto-repo support - use provided URL
                repoUrl = repoUrlEdit.getValue();
                if (!repoUrl.startsWith("http://") && !repoUrl.startsWith("https://")) {
                    repoUrl = "https://" + repoUrl;
                }
                if (!repoUrl.endsWith(".git")) {
                    repoUrl = repoUrl + ".git";
                }
            }

            // Git init on world save folder
            progressScreen.beginTask("Create Git repo", 0);
            boolean ok = GitManager.init(minecraft, level.getLevelId(), repoUrl, progressScreen);
            if (!ok) {
                minecraft.submit(() -> {
                    SystemToast.add(minecraft.gui.toastManager(), new SystemToast.SystemToastId(), Component.translatable("minegit.sync.enable.git_init.error"), null);
                    minecraft.gui.setScreen(this);
                    cancelButton.active = true;
                });
                return;
            }

            SystemToast.add(minecraft.gui.toastManager(), new SystemToast.SystemToastId(), Component.translatable("minegit.sync.enable.complete"), null);
            minecraft.submit(this::onClose);
        }).start();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
        if (closeCallback != null) closeCallback.run();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
    }
}
