package upload;

import fernet.FernetKeyPair;
import fernet.KeyGenerator;
import models.NodeInfo;
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
     * @param node The node information containing NodeId and NodePort.
     * @return A Transaction object containing details about the upload.
     * @throws Exception If any error occurs during the process.
     */
    public static Transaction upload(String filePath, NodeInfo node) throws Exception {

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
        String fileContent = Files.readString(Paths.get("keys/" + node.getNodeId() + ".txt"));

        String pubKey = fileContent.lines()
                .filter(line -> line.startsWith("publicKey="))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("publicKey not found"));

        String[] pubParts = pubKey.split("=")[1].split(",");
        BigInteger e = new BigInteger(pubParts[0]);
        BigInteger n = new BigInteger(pubParts[1]);

        String privKey = fileContent.lines()
                .filter(line -> line.startsWith("privateKey="))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("privateKey not found"));
        String[] privParts = privKey.split("=")[1].split(",");
        BigInteger d = new BigInteger(privParts[0]);

        try {
            BigInteger fileKey = new BigInteger(fernKey.getBytes(StandardCharsets.UTF_8));
            eFileKey = EncryptDecrypt.encrypt(fileKey, e, n);

            System.out.println("STEP 3: SUCCESS");
        } catch (Exception ex) {
            throw new RuntimeException("Failed to encrypt the fileKey: " + ex.getMessage(), ex);
        }

        // STEP 4: UPLOAD FILE TO IPFS AND GET FILE HASH (CID)
        String fileHash;

        try {
            IPFSUtil ipfsUtil = new IPFSUtil("/ip4/127.0.0.1/tcp/5001");
            fileHash = ipfsUtil.upload(fileOutPath);

            System.out.println("STEP 4: SUCCESS fileHash: " + fileHash);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload on IPFS: " + ex.getMessage(), ex);
        }

        // STEP 5: CREATE A TRANSACTION
        String bFileName = fileName;
        String bFileHash = fileHash;
        String bEncryptedFileKey = eFileKey.toString();
        String bTransactionType = "upload";
        Transaction transaction = new Transaction(node, null, bFileName, bFileHash, pubKey, null,
                bEncryptedFileKey, bTransactionType, null, null);

        System.out.println("STEP 5: SUCCESS");

        // STEP 6: SIGN TRANSACTION WITH USER'S PRIVATE KEY
        try {
            String transactionStr = transaction.toString();
            // Convert the transaction string to a BigInteger
            BigInteger transactionBigInt = new BigInteger(transactionStr.getBytes(StandardCharsets.UTF_8));

            // Check if the transactionHash is less than n, if not we use modulus to make it smaller than n
            if (transactionBigInt.compareTo(n) >= 0) {
                transactionBigInt = transactionBigInt.mod(n);
            }

            String signedTransaction = EncryptDecrypt.sign(transactionBigInt, d, n);

            // Convert signedTransaction to string and set it in the transaction
            transaction.setCreatorSign(signedTransaction);
            System.out.println("STEP 6: SUCCESS creatorsSign");

        } catch (Exception ex) {
            throw new RuntimeException("Failed to sign the transaction: " + ex.getMessage(), ex);
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

        NodeInfo dummyNode = new NodeInfo("user1", 8000, 0, 0);

        Transaction transaction = upload(filePath, dummyNode);
        System.out.println("Created Transaction: " + transaction);
    }
}