package ca.modmonster.minegit.gui;

import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import ca.modmonster.minegit.data.Config;
import ca.modmonster.minegit.data.ConfigManager;
import ca.modmonster.minegit.data.CryptoManager;
import ca.modmonster.minegit.data.NetworkManager;

public class AccountLinkScreen extends Screen {
    private static final Component USERNAME_EDIT_LABEL = Component.translatable("minegit.link.username");
    private static final Component PAT_EDIT_LABEL = Component.translatable("minegit.link.pat");
    private static final Identifier RALSPIN = Identifier.fromNamespaceAndPath("minegit", "ralspin");
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 8 + 9 + 8 + 20 + 4, 60);

    private final Screen parent;
    private EditBox usernameEdit;
    private EditBox patEdit;
    private Button testCredentialsButton;
    private ImageWidget ralspinWidget;
    private boolean requestInProgress = false;
    private StringWidget testCredentialsStatus;

    public AccountLinkScreen(Screen parent) {
        super(Component.translatable("minegit.link.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Column layout
        LinearLayout columnLayout = this.layout.addToContents(LinearLayout.vertical().spacing(8));
        columnLayout.defaultCellSetting().alignHorizontallyCenter();

        // Menu title
        layout.addTitleHeader(this.title, this.font);

        // Username text field
        StringWidget usernameEditLabel = columnLayout.addChild(new StringWidget(USERNAME_EDIT_LABEL, font));
        usernameEditLabel.setAlpha(0.5f);
        usernameEdit = new EditBox(font, 0, 0, 200, 20, USERNAME_EDIT_LABEL);
        usernameEdit.setMaxLength(39);
        usernameEdit.setResponder(string -> updateTestButtonStatus(false));
        columnLayout.addChild(usernameEdit);

        // PAT text field
        StringWidget patEditLabel = columnLayout.addChild(new StringWidget(PAT_EDIT_LABEL, font));
        patEditLabel.setAlpha(0.5f);
        patEdit = new EditBox(font, 0, 0, 200, 20, PAT_EDIT_LABEL);
        patEdit.setMaxLength(255);
        patEdit.setResponder(string -> updateTestButtonStatus(false));
        columnLayout.addChild(patEdit);

        // Test credentials button
        testCredentialsButton = Button.builder(Component.translatable("minegit.link.test"), button -> testCredentials()).size(200, 20).build();
        columnLayout.addChild(testCredentialsButton);

        // Test credentials status
        testCredentialsStatus = new StringWidget(Component.empty(), font);
        columnLayout.addChild(testCredentialsStatus);

        // Add layout widgets
        this.layout.visitWidgets(this::addRenderableWidget);
        this.layout.arrangeElements();

        // Back button
        Button backButton = Button.builder(Component.literal("←"), button -> onClose())
            .tooltip(Tooltip.create(Component.translatable("minegit.link.back")))
            .bounds(6, 6, 20, 20)
            .build();
        addRenderableWidget(backButton);

        // Ralsei go spinny
        ralspinWidget = ImageWidget.sprite(42, 80, RALSPIN);
        ralspinWidget.setPosition(width - 60, height - 80);
        ralspinWidget.setTooltip(Tooltip.create(Component.literal("hiiiii!! ^-^")));
        addRenderableWidget(ralspinWidget);

        updateTestButtonStatus(false);

        // Load configuration and update default values
        Config config = ConfigManager.getCurrentConfig();
        usernameEdit.setValue(config.username);
        String pat = CryptoManager.decrypt(config.patEncrypted);
        if (pat != null) patEdit.setValue(pat);
    }

    private void testCredentials() {
        requestInProgress = true;
        updateTestButtonStatus(false);
        int statusCode = NetworkManager.testCredentials(usernameEdit.getValue(), patEdit.getValue());
        requestInProgress = false;
        updateTestButtonStatus(false);

        switch (statusCode) {
            case -1:
                updateTestCredentialsStatus(Component.translatable("minegit.link.status.error_generic"));
                updateTestButtonStatus(true);
                break;
            case 200:
                updateTestCredentialsStatus(Component.translatable("minegit.link.status.success"));
                updateTestButtonStatus(true);
                break;
            case 401:
                updateTestCredentialsStatus(Component.translatable("minegit.link.status.error_pat"));
                updateTestButtonStatus(true);
                break;
            case 404:
                updateTestCredentialsStatus(Component.translatable("minegit.link.status.error_username"));
                updateTestButtonStatus(true);
                break;
        }
    }

    private void updateTestButtonStatus(boolean forceDisable) {
        testCredentialsButton.active = !forceDisable && !requestInProgress && !usernameEdit.getValue().isBlank() && !patEdit.getValue().isBlank();
    }

    private void updateTestCredentialsStatus(Component message) {
        testCredentialsStatus.setMessage(message);
        layout.arrangeElements();
    }

    @Override
    public void onClose() {
        // Save credentials
        String username = usernameEdit.getValue();
        String pat = CryptoManager.encrypt(patEdit.getValue());
        ConfigManager.save(new Config(username, pat));
        minecraft.setScreen(parent);
    }

    @Override
    protected void setInitialFocus() {
        setInitialFocus(usernameEdit);
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        ralspinWidget.setPosition(width - 60, height - 80);
    }
}
