package ca.modmonster.minegit.data;

import net.minecraft.client.Minecraft;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import ca.modmonster.minegit.MineGIT;

public class GitManager {
    public static boolean syncEnabled(Minecraft minecraft, String worldId) {
        return syncEnabled(getPath(minecraft, worldId));
    }

    public static boolean syncEnabled(Path worldFolder) {
        return worldFolder.resolve(".git").toFile().exists();
    }

    public static boolean pull(Minecraft minecraft, String worldId) {
        Path worldFolder = getPath(minecraft, worldId);
        try (Git git = Git.open(worldFolder.toFile())) {
            git.pull().call();
            return true;
        } catch (IOException | GitAPIException e) {
            MineGIT.LOGGER.error("Error pulling from repo", e);
            return false;
        }
    }

    public static boolean push(Minecraft minecraft, String worldId) {
        return push(getPath(minecraft, worldId));
    }

    public static boolean push(Path worldFolder) {
        Config config = ConfigManager.getCurrentConfig();
        try (Git git = Git.open(worldFolder.toFile())) {
            // add all
            git.add()
                    .addFilepattern(".")
                    .call();
            // commit
            String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a, MM/dd/yy"));
            git.commit()
                    .setMessage("World snapshot - " + timestamp)
                    .call();
            // push (TODO: show progress)
            git.push()
                    .setRemote("origin")
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(config.username, config.getPat()))
                    .call();
            return true;
        } catch (GitAPIException | IOException e) {
            MineGIT.LOGGER.error("Error with Git repo", e);
            return false;
        }
    }

    public static boolean init(Minecraft minecraft, String worldId, String repoUrl) {
        Path worldFolder = getPath(minecraft, worldId);
        Config config = ConfigManager.getCurrentConfig();
        try (Git git = Git.init().setDirectory(worldFolder.toFile()).call()) {
            // add all
            git.add()
                    .addFilepattern(".")
                    .call();
            // commit
            String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a, MM/dd/yy"));
            git.commit()
                    .setMessage("Initial world snapshot - " + timestamp)
                    .call();
            // create branch
            git.checkout()
                    .setName("main")
                    .setCreateBranch(true)
                    .call();
            // add remote
            git.remoteAdd()
                    .setName("origin")
                    .setUri(new URIish(repoUrl))
                    .call();
            // push (TODO: show progress)
            git.push()
                    .setRemote("origin")
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(config.username, config.getPat()))
                    .call();
            return true;
        } catch (GitAPIException | URISyntaxException e) {
            MineGIT.LOGGER.error("Error with Git repo", e);
            return false;
        }
    }

    private static Path getPath(Minecraft minecraft, String worldId) {
        return minecraft.getLevelSource().getBaseDir().resolve(worldId);
    }
}
