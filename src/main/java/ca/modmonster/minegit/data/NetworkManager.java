package ca.modmonster.minegit.data;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NetworkManager {
    public static boolean hasValidCredentials = false;

    private static final HttpClient client;
    static {
        try {
            client = HttpClient.newBuilder().build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize HTTP client", e);
        }
    }

    public static int testCredentials(String username, String pat) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/users/" + username))
                .header("Authorization", "token " + pat)
                .GET().build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            hasValidCredentials = status == 200;
            return status;
        } catch (IOException | InterruptedException e) {
            hasValidCredentials = false;
            return -1;
        }
    }

    public static HttpResponse<String> createRepo(String pat, String worldId, String worldName) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/user/repos"))
                .header("Authorization", "token " + pat)
                .header("X-GitHub-Api-Version", "2026-03-10")
                .header("Accept", "application/vnd.github+json")
                .POST(HttpRequest.BodyPublishers.ofString(String.format("{\"name\":\"minegit_%s\",\"description\":\"Minecraft save for %s. Cloud sync by MineGIT\",\"private\":true}", worldId, worldName))).build();

        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }
}
