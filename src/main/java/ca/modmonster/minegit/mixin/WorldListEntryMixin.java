package ca.modmonster.minegit.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import ca.modmonster.minegit.MineGIT;
import ca.modmonster.minegit.data.GitManager;

@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class WorldListEntryMixin {
    @Shadow
    public abstract LevelSummary getLevelSummary();

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private WorldSelectionList list;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Inject(method = "doDeleteWorld", at = @At("HEAD"))
    private void beforeWorldDelete(CallbackInfo ci) {
        Path gitFolder = minecraft.getLevelSource().getBaseDir().resolve(getLevelSummary().getLevelId()).resolve(".git");

        // Delete .git folder
        if (!gitFolder.toFile().exists()) return;
        try (Stream<Path> files = Files.walk(gitFolder)){
            files.forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            MineGIT.LOGGER.error("Failed to delete .git folder", e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Inject(method = "joinWorld", at = @At("HEAD"), cancellable = true)
    private void beforeWorldJoin(CallbackInfo ci) {
        String worldId = getLevelSummary().getLevelId();
        if (!GitManager.syncEnabled(minecraft, worldId)) return;
        ci.cancel();
        minecraft.setScreen(new GenericMessageScreen(Component.translatable("minegit.sync.status.git_pull")));
        new Thread(() -> {
            boolean ok = GitManager.pull(minecraft, worldId);
            if (ok) {
                // Continue loading the world
                minecraft.submit(() -> minecraft.createWorldOpenFlows().openWorld(getLevelSummary().getLevelId(), list::returnToScreen));
            } else {
                // Show toast saying "error :("
                minecraft.setScreen(null);
                minecraft.getToastManager().addToast(new SystemToast(new SystemToast.SystemToastId(), Component.translatable("minegit.sync.status.git_pull_error"), null));
            }
        }).start();
    }
}
