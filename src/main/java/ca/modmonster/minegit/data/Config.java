package ca.modmonster.minegit.data;

public class Config {
    public String username;
    public String patEncrypted;

    public Config(String username, String patEncrypted) {
        this.username = username;
        this.patEncrypted = patEncrypted;
    }

    public Config() {
        this("", "");
    }
}
