package fernet;

import utils.WriteKeysUtil;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

public class KeyGenerator {
    public String getFernKey() {
        byte[] key = new byte[32]; // 32 bytes = 256 bits
        SecureRandom random = new SecureRandom();
        random.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    public static void main(String[] args) {
        if (args.length != 1 || args[0].startsWith("-help")) {
            System.out.println("Usage:  java fernet.KeyGenerator getFernetKey");
            return;
        }

        if (Objects.equals(args[0], "getFernetKey")) {
            KeyGenerator keyGenerator = new KeyGenerator();
            String fernKey = keyGenerator.getFernKey();
            String filePath = "keys/fileKey.txt";
            WriteKeysUtil.writeKeysToFile("Fernet", filePath, fernKey);
            System.out.println("Fernet key generated and saved to " + filePath);
        } else {
            System.out.println("Use command 'getFernetKey' to generate fernet key.");
        }
    }
}
