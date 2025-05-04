package fernet;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class EncryptDecrypt {

    public static FernetKeyPair getKeysFromFile(String keyFilePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(keyFilePath));
        String line = reader.readLine();
        reader.close();

        String base64Key = line.split("=")[1].trim();
        byte[] fullKey = Base64.getDecoder().decode(base64Key);

        if (fullKey.length != 32) {
            throw new IllegalArgumentException("Fernet key must be 32 bytes.");
        }

        SecretKeySpec aesKey = new SecretKeySpec(fullKey, 0, 16, "AES");
        SecretKeySpec hmacKey = new SecretKeySpec(fullKey, 16, 16, "HmacSHA256");
        return new FernetKeyPair(aesKey, hmacKey);
    }

    public static String encrypt(byte[] plaintext, SecretKey aesKey, SecretKey hmacKey) throws Exception {
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
        byte[] ciphertext = cipher.doFinal(plaintext);

        ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
        dataOut.write(0x80); // Fernet version
        long timestamp = System.currentTimeMillis() / 1000;
        dataOut.write(ByteBuffer.allocate(8).putLong(timestamp).array());
        dataOut.write(iv);
        dataOut.write(ciphertext);
        byte[] data = dataOut.toByteArray();

        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(hmacKey);
        byte[] hmacValue = hmac.doFinal(data);

        ByteArrayOutputStream finalOut = new ByteArrayOutputStream();
        finalOut.write(data);
        finalOut.write(hmacValue);
        return Base64.getEncoder().encodeToString(finalOut.toByteArray());
    }

    public static byte[] decrypt(String base64Ciphertext, SecretKey aesKey, SecretKey hmacKey) throws Exception {
        byte[] token = Base64.getDecoder().decode(base64Ciphertext);
        int hmacStart = token.length - 32;

        byte[] data = new byte[hmacStart];
        byte[] receivedHmac = new byte[32];
        System.arraycopy(token, 0, data, 0, hmacStart);
        System.arraycopy(token, hmacStart, receivedHmac, 0, 32);

        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(hmacKey);
        byte[] expectedHmac = hmac.doFinal(data);

        if (!MessageDigest.isEqual(receivedHmac, expectedHmac)) {
            throw new SecurityException("HMAC verification failed. Data may have been tampered with.");
        }

        byte[] iv = new byte[16];
        System.arraycopy(data, 9, iv, 0, 16);
        byte[] ciphertext = new byte[data.length - 25];
        System.arraycopy(data, 25, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
        return cipher.doFinal(ciphertext);
    }

    public static void encryptFile(String inputPath, String outputPath, SecretKey aesKey, SecretKey hmacKey) throws Exception {
        byte[] plaintext = readAllBytes(inputPath);
        String token = encrypt(plaintext, aesKey, hmacKey);
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(token);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void decryptFile(String inputPath, String outputPath, SecretKey aesKey, SecretKey hmacKey) throws Exception {
        String base64Token = new String(readAllBytes(inputPath), StandardCharsets.UTF_8);
        byte[] decrypted = decrypt(base64Token, aesKey, hmacKey);
        try (FileOutputStream out = new FileOutputStream(outputPath)) {
            out.write(decrypted);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] readAllBytes(String path) throws IOException {
        try (FileInputStream in = new FileInputStream(path)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        String keyFilePath;
        String filePath;

        if (args.length != 2 || args[0].startsWith("-help")) {
            System.out.println("Usage: java fernet.EncryptDecrypt <keyFilePath> <filePath>");
            keyFilePath = "keys/fileKey.txt";
            filePath = "files/test.txt";
//            return;
        } else {
            keyFilePath = args[0];
            filePath = args[1];
        }

        // Get the file name from the file path
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        String basePath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
        String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf("."));
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);

        // Split inputFile to get the file extension
        String[] filenameParts = fileName.split("\\.");
        String encryptedFile = filenameParts[0] +  "_encrypted." + filenameParts[1];
        String decryptedFile = filenameParts[0] + "_decrypted." + filenameParts[1];

        try {
            FernetKeyPair keyPair = getKeysFromFile(keyFilePath);
            encryptFile(basePath + fileName, basePath + encryptedFile, keyPair.aesKey, keyPair.hmacKey);
            decryptFile(basePath + encryptedFile, basePath + decryptedFile, keyPair.aesKey, keyPair.hmacKey);
            System.out.println("files encrypted and decrypted successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
