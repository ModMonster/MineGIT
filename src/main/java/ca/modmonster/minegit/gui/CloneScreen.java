package ca.modmonster.minegit.gui;

import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import ca.modmonster.minegit.data.GitManager;

public class CloneScreen extends Screen {
    private static final Component REPO_LABEL = Component.translatable("minegit.clone.repo");
    private static final Identifier RALSPIN = Identifier.fromNamespaceAndPath("minegit", "ralspin");
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 8 + 9 + 8 + 20 + 4, 60);

    private final Screen parent;
    private final Runnable closeCallback;
    private EditBox repoEdit;
    private Button testCredentialsButton;
    private ImageWidget ralspinWidget;
    private StringWidget testCredentialsStatus;
    private boolean requestInProgress = false;

    public CloneScreen(Screen parent) {
        this(parent, null);
    }

    public CloneScreen(Screen parent, Runnable closeCallback) {
        super(Component.translatable("minegit.clone.title"));
        this.parent = parent;
        this.closeCallback = closeCallback;
    }

    @Override
    protected void init() {
        // Column layout
        LinearLayout columnLayout = this.layout.addToContents(LinearLayout.vertical().spacing(8));
        columnLayout.defaultCellSetting().alignHorizontallyCenter();

        // Menu title
        layout.addTitleHeader(this.title, this.font);

        // Repo name text field
        StringWidget usernameEditLabel = columnLayout.addChild(new StringWidget(REPO_LABEL, font));
        usernameEditLabel.setAlpha(0.5f);
        repoEdit = new EditBox(font, 0, 0, 200, 20, REPO_LABEL);
        repoEdit.setMaxLength(39);
        repoEdit.setResponder(string -> updateTestButtonStatus());
        columnLayout.addChild(repoEdit);

        // Clone button
        testCredentialsButton = Button.builder(Component.translatable("minegit.clone.confirm"), button -> doClone()).size(200, 20).build();
        columnLayout.addChild(testCredentialsButton);

        // Clone status
        testCredentialsStatus = new StringWidget(Component.empty(), font);
        columnLayout.addChild(testCredentialsStatus);

        // Add layout widgets
        this.layout.visitWidgets(this::addRenderableWidget);
        this.layout.arrangeElements();

        // Back button
        Button backButton = Button.builder(Component.literal("←"), button -> onClose())
            .tooltip(Tooltip.create(Component.translatable("minegit.clone.back")))
            .bounds(6, 6, 20, 20)
            .build();
        addRenderableWidget(backButton);

        // Ralsei go spinny
        ralspinWidget = ImageWidget.sprite(42, 80, RALSPIN);
        ralspinWidget.setPosition(width - 60, height - 80);
        ralspinWidget.setTooltip(Tooltip.create(Component.literal("hiiiii!! ^-^")));
        addRenderableWidget(ralspinWidget);

        updateTestButtonStatus();
    }

    private void doClone() {
        requestInProgress = true;
        testCredentialsStatus.setMessage(Component.translatable("minegit.clone.in_progress"));
        repositionElements();
        updateTestButtonStatus();
        int result = GitManager.cloneRepo(minecraft, repoEdit.getValue());
        requestInProgress = false;
        if (result == 0) {
            minecraft.getToastManager().addToast(new SystemToast(new SystemToast.SystemToastId(), Component.translatable("minegit.clone.success"), null));
            onClose();
        } else if (result == 1) {
            testCredentialsStatus.setMessage(Component.translatable("minegit.clone.error_invalid_remote"));
            repositionElements();
            updateTestButtonStatus();
        } else {
            testCredentialsStatus.setMessage(Component.translatable("minegit.clone.error_generic"));
            repositionElements();
            updateTestButtonStatus();
        }
    }

    private void updateTestButtonStatus() {
        testCredentialsButton.active = !requestInProgress && !repoEdit.getValue().isBlank();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
        if (closeCallback != null) closeCallback.run();
    }

    @Override
    protected void setInitialFocus() {
        setInitialFocus(repoEdit);
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        ralspinWidget.setPosition(width - 60, height - 80);
    }
}
