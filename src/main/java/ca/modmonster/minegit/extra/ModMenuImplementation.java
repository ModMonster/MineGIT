package ca.modmonster.minegit.extra;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import ca.modmonster.minegit.gui.AccountLinkScreen;

public class ModMenuImplementation implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AccountLinkScreen::new;
    }
}
