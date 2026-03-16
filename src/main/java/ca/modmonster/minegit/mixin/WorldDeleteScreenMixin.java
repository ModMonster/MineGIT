package ca.modmonster.minegit.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
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

@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class WorldDeleteScreenMixin {
    @Shadow
    public abstract LevelSummary getLevelSummary();

    @Shadow
    @Final
    private Minecraft minecraft;

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
}
