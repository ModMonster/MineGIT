package ca.modmonster.minegit.data;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import ca.modmonster.minegit.MineGIT;

public class CryptoManager {
    public static byte[] machineKey = null;

    public static String encrypt(String input) {
        try {
            SecretKeySpec key = new SecretKeySpec(getMachineKey(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encrypted = cipher.doFinal(input.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed. RIP in peace", e);
        }
    }

    public static String decrypt(String base64) {
        try {
            SecretKeySpec key = new SecretKeySpec(getMachineKey(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] decoded = Base64.getDecoder().decode(base64);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            MineGIT.LOGGER.warn("Decryption of saved PAT failed. This could be due to hardware or environment changes, or runtime issues.");
            return null;
        }
    }

    public static byte[] getMachineKey() {
        if (machineKey != null) return machineKey;

        String[] properties = {"os.arch", "user.name", "user.home", "sun.cpu.endian", "sun.cpu.isalist"};
        String[] environmentVariables = {"COMPUTERNAME", "PROCESSOR_ARCHITECTURE", "PROCESSOR_REVISION", "PROCESSOR_IDENTIFIER", "PROCESSOR_LEVEL", "NUMBER_OF_PROCESSORS", "OS", "USERNAME", "USERDOMAIN", "APPDATA", "HOMEPATH", "LOCALAPPDATA"};

        StringBuilder fingerprint = new StringBuilder();
        fingerprint.append(Runtime.getRuntime().availableProcessors());

        // Add system properties to fingerprint
        for (String property : properties) {
            fingerprint.append(System.getProperty(property));
        }

        // Add environment variables to fingerprint
        for (String var : environmentVariables) {
            fingerprint.append(System.getenv(var));
        }

        // Save a randomly generated key to a file
        Path keyFilePath = FabricLoader.getInstance().getConfigDir().resolve(".minegit.key");
        String fileKey = null;
        if (keyFilePath.toFile().exists()) {
            try {
                fileKey = Files.readString(keyFilePath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read from key file", e);
            }

            // Set file as hidden on Windows
            try {
                Files.setAttribute(keyFilePath, "dos:hidden", true);
            } catch (IOException ignored) {}
        } else {
            byte[] key = new byte[32];
            new SecureRandom().nextBytes(key);
            String encoded = Base64.getEncoder().encodeToString(key);
            try {
                Files.writeString(keyFilePath, encoded, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write to key file", e);
            }
        }
        if (fileKey == null) throw new RuntimeException("Something went wrong with the key file.");
        fingerprint.append(fileKey);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            machineKey = digest.digest(fingerprint.toString().getBytes());
            return machineKey;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available. Your Java runtime is fundamentally broken.", e);
        }
    }
}
