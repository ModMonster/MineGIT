package ca.modmonster.minegit.data;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

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

        String[] properties = {"os.arch", "java.io.tmpdir", "native.encoding", "user.name", "user.home", "user.country", "sun.io.unicode.encoding", "stderr.encoding", "sun.cpu.endian", "sun.cpu.isalist", "sun.jnu.encoding", "stdout.encoding", "sun.arch.data.model", "user.language", "user.variant"};
        String[] environmentVariables = {"COMPUTERNAME", "PROCESSOR_ARCHITECTURE", "PROCESSOR_REVISION", "PROCESSOR_IDENTIFIER", "PROCESSOR_LEVEL", "NUMBER_OF_PROCESSORS", "OS", "USERNAME", "USERDOMAIN", "USERDOMAIN_ROAMINGPROFILE", "APPDATA", "HOMEPATH", "LOGONSERVER", "LOCALAPPDATA", "TEMP", "TMP"};

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

        try {
            List<NetworkInterface> networkInterfaces = NetworkInterface.networkInterfaces().toList();
            for (NetworkInterface net : networkInterfaces) {
                fingerprint.append(net.getName());
                fingerprint.append(net.getDisplayName());
                fingerprint.append(Arrays.toString(net.getHardwareAddress()));
                fingerprint.append(net.getMTU());
            }
        } catch (SocketException e) {
            MineGIT.LOGGER.info("No network interfaces found for encryption");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            machineKey = digest.digest(fingerprint.toString().getBytes());
            return machineKey;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available. Your Java runtime is fundamentally broken.", e);
        }
    }
}
