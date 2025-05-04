package upload;

import fernet.FernetKeyPair;
import fernet.KeyGenerator;
import models.Transaction;
import rsa.EncryptDecrypt;
import utils.IPFSUtil;
import utils.WriteKeysUtil;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static fernet.EncryptDecrypt.*;

public class Upload {

    /**
     * Uploads a file to IPFS, encrypts it with a Fernet key, and creates a transaction.
     *
     * @param filePath The path to the file to be uploaded.
     * @param username The username of the user uploading the file.
     * @return A Transaction object containing details about the upload.
     * @throws Exception If any error occurs during the process.
     */
    public static Transaction upload(String filePath, String username) throws Exception {

        // If fileName is not provided, fall back to the default
        String fileName;
        String basePath = "files/";

        if (filePath == null || filePath.isEmpty()) {
            System.out.println("File path not provided. Using default: test.txt");
            fileName = "test.txt";
        } else {
            fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        }

        // STEP 1: GENERATE FERN KEY
        String fernKey = null;
        String fernFilePath = "keys/fileKey.txt";

        try {
            KeyGenerator fernKeyGen = new KeyGenerator();
            fernKey = fernKeyGen.getFernKey();
            fernFilePath = "keys/fileKey.txt";
            WriteKeysUtil.writeKeysToFile("Fernet", fernFilePath, fernKey);

            System.out.println("STEP 1: SUCCESS");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate/write Fernet key: " + e.getMessage(), e);
        }

        // STEP 2: ENCRYPT FILE WITH FERNET KEY
        String fileOutPath;
        try {
            FernetKeyPair keyPair = getKeysFromFile(fernFilePath);
            String[] filenameParts = fileName.split("\\.");
            fileOutPath = basePath + filenameParts[0] + "_encrypted." + filenameParts[1];
            encryptFile(basePath + fileName, fileOutPath, keyPair.aesKey, keyPair.hmacKey);

            System.out.println("STEP 2: SUCCESS");
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt the file: " + e.getMessage(), e);
        }

        // STEP 3: ENCRYPT FERNET KEY (FILE KEY) WITH USER'S PUBLIC KEY
        BigInteger eFileKey;
        String fileContent = Files.readString(Paths.get("keys/" + username + ".txt"));

        String pubKey = fileContent.lines()
                .filter(line -> line.startsWith("publicKey="))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("publicKey not found"));

        String privKey = fileContent.lines()
                .filter(line -> line.startsWith("privateKey="))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("privateKey not found"));

        try {
            String[] pubParts = pubKey.split("=")[1].split(",");
            BigInteger e = new BigInteger(pubParts[0]);
            BigInteger n = new BigInteger(pubParts[1]);

            BigInteger fileKey = new BigInteger(fernKey.getBytes(StandardCharsets.UTF_8));
            eFileKey = EncryptDecrypt.encrypt(fileKey, e, n);

            System.out.println("STEP 3: SUCCESS");
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt the fileKey: " + e.getMessage(), e);
        }

        // STEP 4: UPLOAD FILE TO IPFS AND GET FILE HASH (CID)
        String fileHash;

        try {
            IPFSUtil ipfsUtil = new IPFSUtil("/ip4/127.0.0.1/tcp/5001");
            fileHash = ipfsUtil.upload(fileOutPath);

            System.out.println("STEP 4: SUCCESS fileHash: " + fileHash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload on IPFS: " + e.getMessage(), e);
        }

        // STEP 5: CREATE A TRANSACTION
        Transaction transaction = new Transaction(
                pubKey.split("=")[1],
                "none",
                fileHash,
                eFileKey.toString(),
                System.currentTimeMillis(),
                "upload",
                "",
                "none"
        );
        System.out.println("STEP 5: SUCCESS");

        // STEP 6: SIGN TRANSACTION WITH USER'S PRIVATE KEY
        try {
            String[] privParts = privKey.split("=")[1].split(",");
            BigInteger d = new BigInteger(privParts[0]);
            BigInteger n = new BigInteger(privParts[1]);

            String transactionHash = transaction.toString();
            BigInteger transactionHashBigInt = new BigInteger(transactionHash.getBytes(StandardCharsets.UTF_8));
            // Handle the case where transactionHashBigInt is greater than n
            if (transactionHashBigInt.compareTo(n) >= 0) {
                transactionHashBigInt = transactionHashBigInt.mod(n);
            }
            BigInteger signedTransaction = EncryptDecrypt.encrypt(transactionHashBigInt, d, n);

            // Convert signedTransaction to string and set it in the transaction
            transaction.setCreatorSign(signedTransaction.toString());
            System.out.println("STEP 6: SUCCESS creatorsSign: " + transaction.getCreatorSign());

        } catch (Exception e) {
            throw new RuntimeException("Failed to sign the transaction: " + e.getMessage(), e);
        }

        // Return the created transaction
        return transaction;
    }

    public static void main(String[] args) throws Exception {
        String filePath;

        if (args.length != 1 || args[0].startsWith("-help")) {
            System.out.println("Usage: java upload.Upload <filePath>");
            filePath = "files/test.txt";
        } else {
            filePath = args[0];
        }

        Transaction transaction = upload(filePath,"user1");
        System.out.println("Created Transaction: " + transaction);
    }
}