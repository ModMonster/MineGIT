package ca.modmonster.minegit.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

import ca.modmonster.minegit.MineGIT;
import ca.modmonster.minegit.data.GitManager;
import ca.modmonster.minegit.gui.GitProgressScreen;

@Environment(EnvType.CLIENT)
@Mixin(IntegratedServer.class)
public class LevelSaveMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Inject(method = "stopServer", at = @At("TAIL"))
    private void onWorldSaved(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        Path worldFolder = server.getWorldPath(LevelResource.ROOT); // get world folder
        if (!GitManager.syncEnabled(worldFolder)) return;
        MineGIT.LOGGER.info("Pushing current world to GitHub");

        minecraft.submit(() -> {
            GitProgressScreen progressScreen = new GitProgressScreen(Component.translatable("minegit.sync.status.git_push"));
            minecraft.setScreen(progressScreen);
            new Thread(() -> {
                boolean ok = GitManager.push(worldFolder, progressScreen);
                if (!ok) {
                    minecraft.getToastManager().addToast(new SystemToast(new SystemToast.SystemToastId(), Component.translatable("minegit.sync.status.git_push_error"), null));
                }
                minecraft.submit(() -> minecraft.setScreen(null));
            }).start();
        });
    }
}
