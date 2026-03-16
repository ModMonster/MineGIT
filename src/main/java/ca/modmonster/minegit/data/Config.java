package ca.modmonster.minegit.data;

public class Config {
    public String username;
    public String patEncrypted;
    private transient String pat;

    public Config(String username, String patEncrypted) {
        this.username = username;
        this.patEncrypted = patEncrypted;
    }

    public Config() {
        this("", "");
    }

    public String getPat() {
        if (pat == null) {
            pat = CryptoManager.decrypt(patEncrypted);
            if (pat == null) pat = "";
        }
        return pat;
    }
}
