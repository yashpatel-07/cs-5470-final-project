package download;

import utils.IPFSUtil;

import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class Download {
    /**
     * Downloads a file from IPFS using the provided file hash, eFilekey, and username.
     *
     * @param fileHash The hash of the file to be downloaded.
     * @param eFilekey The encrypted file key used for decryption.
     * @param username The username of the user downloading the file.
     * @throws Exception If any error occurs during the download or decryption process.
     */
    public static void download(String fileName, String fileHash, String eFilekey, String username) {
        // STEP 1: DOWNLOAD FILE FROM IPFS
        String basePath = "files/";
        try {
            IPFSUtil ipfsUtil = new IPFSUtil("/ip4/127.0.0.1/tcp/5001");
            ipfsUtil.download(fileHash, basePath + "encrypted_" + fileName);
            System.out.println("STEP 1: SUCCESS - File downloaded from IPFS");
        } catch (Exception e) {
            throw new RuntimeException("Failed to download the file: " + e.getMessage(), e);
        }

        // STEP 2: DECRYPT THE EFILEKEY USING USER'S PRIVATE KEY
        String decryptedFileKey = null;
        try {
            BigInteger eFileKey;
            String fileContent = Files.readString(Paths.get("keys/" + username + ".txt"));

            String privKey = fileContent.lines()
                    .filter(line -> line.startsWith("privateKey="))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("privateKey not found"));

            String[] privParts = privKey.split("=")[1].split(",");

            BigInteger d = new BigInteger(privParts[0]);
            BigInteger n = new BigInteger(privParts[1]);
            decryptedFileKey = (String) rsa.EncryptDecrypt.decrypt(new BigInteger(eFilekey), d, n, true);
            System.out.println("STEP 2: SUCCESS - File key decrypted " + decryptedFileKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt the file key: " + e.getMessage(), e);
        }

        // STEP 2: CONVERT THE DECRYPTED FILE KEY TO AES AND HMAC KEYS
        SecretKeySpec aesKey;
        SecretKeySpec hmacKey;
        try {
            byte[] fullKey = Base64.getDecoder().decode(decryptedFileKey);

            if (fullKey.length != 32) {
                throw new IllegalArgumentException("Fernet key must be 32 bytes.");
            }

            aesKey = new SecretKeySpec(fullKey, 0, 16, "AES");
            hmacKey = new SecretKeySpec(fullKey, 16, 16, "HmacSHA256");
            System.out.println("STEP 2: SUCCESS - Converted decrypted file key to AES and HMAC keys");
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert the decrypted file key to AES and HMAC keys: " + e.getMessage(), e);
        }

        // STEP 3: DECRYPT THE FILE USING THE DECRYPTED FILE KEY
        try {
            String inputPath = basePath + "encrypted_" + fileName;
            String outPath = basePath + "decrypted_" + fileName;
            fernet.EncryptDecrypt.decryptFile(inputPath, outPath, aesKey, hmacKey);
            System.out.println("STEP 3: SUCCESS - File decrypted at " + outPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt the file: " + e.getMessage(), e);
        }

    }

    public static void main(String[] args) {
        if (args.length != 4 || args[0].startsWith("-help")) {
            System.out.println("Usage: java download.Download <fileName> <fileHash> <eFilekey> <username>");
//            return;
            args = new String[]{"test.txt", "Qmf5ztSr4jNjyzrQJpJ1QtyifdGRpF611mRsrK25VVY93P", "394057147557273929235744935992137414902850444887287784494046217507868918304590670940618394302293680949682110510100942970902052483889599409331407246571091971383578054338681009618919528605997466407836209605003903280153500239669374833956721866871658317218132623277218575404185888020459241066716079241005125857862743883180608573761960700981094636219405710238690787282847108819840689759192859406924353779430525275969238126212159337285309682784874666838451638029270942931004251097039973884519687495196539085716405948945107396893626799322495040440300748949748345019945199463188479887122592713074249836565420259756510133700", "user1"};
        }

        // fileHash = Qmf5ztSr4jNjyzrQJpJ1QtyifdGRpF611mRsrK25VVY93P
        // eFilekey = 394057147557273929235744935992137414902850444887287784494046217507868918304590670940618394302293680949682110510100942970902052483889599409331407246571091971383578054338681009618919528605997466407836209605003903280153500239669374833956721866871658317218132623277218575404185888020459241066716079241005125857862743883180608573761960700981094636219405710238690787282847108819840689759192859406924353779430525275969238126212159337285309682784874666838451638029270942931004251097039973884519687495196539085716405948945107396893626799322495040440300748949748345019945199463188479887122592713074249836565420259756510133700

        String fileName = args[0];
        String fileHash = args[1];
        String eFilekey = args[2];
        String username = args[3];

        try {
            download(fileName, fileHash, eFilekey, username);
        } catch (Exception e) {
            System.err.println("Error during download: " + e.getMessage());
        }

    }

}
