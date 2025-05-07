package download;

import com.google.gson.Gson;
import models.NodeInfo;
import models.Transaction;
import rsa.EncryptDecrypt;
import utils.IPFSUtil;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class Download {

    public static Transaction download(String fileName, String fileHash, String eFilekey, NodeInfo sender, NodeInfo receiver, String type) throws IOException {
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
            String fileContent = Files.readString(Paths.get("keys/" + sender.getNodeId() + ".txt"));

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

        // From here we have two options: 1. Download 2. Share
        if (type == null || type.isEmpty()) {
            System.out.println("Invalid type. Please specify 'download' or 'share'.");
            return null;
        }

        if (type.equals("download")) {
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

            // RETURN THE TRANSACTION OBJECT
            return new Transaction(sender, null, fileName, fileHash, null, null, eFilekey, type, null, null);
        }

        if (type.equals("share")) {
            BigInteger eFileKey;
            String receiverFileContent = Files.readString(Paths.get("keys/" + receiver.getNodeId() + ".txt"));
            String senderFileContent = Files.readString(Paths.get("keys/" + sender.getNodeId() + ".txt"));

            // RECEIVER'S PUBLIC KEY
            String receiverPublicKey = receiverFileContent.lines()
                    .filter(line -> line.startsWith("publicKey="))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("publicKey not found"));
            String[] receiverPubParts = receiverPublicKey.split("=")[1].split(",");
            BigInteger eReceiver = new BigInteger(receiverPubParts[0]);
            BigInteger nReceiver = new BigInteger(receiverPubParts[1]);

            // SENDER'S PUBLIC KEY
            String sendersPublicKey = senderFileContent.lines()
                    .filter(line -> line.startsWith("publicKey="))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("publicKey not found"));
            String[] sendersPubParts = sendersPublicKey.split("=")[1].split(",");
            BigInteger eSender = new BigInteger(sendersPubParts[0]);
            BigInteger nSender = new BigInteger(sendersPubParts[1]);

            // SENDERS PRIVATE KEY
            String sendersPrivateKey = senderFileContent.lines()
                    .filter(line -> line.startsWith("privateKey="))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("privateKey not found"));
            String[] sendersPrivParts = sendersPrivateKey.split("=")[1].split(",");
            BigInteger dSender = new BigInteger(sendersPrivParts[0]);

            // STEP 2: ENCRYPT THE FILE KEY USING RECEIVER'S PUBLIC KEY
            BigInteger eReceiverFileKey;
            try {
                BigInteger fileKey = new BigInteger(decryptedFileKey.getBytes());
                eReceiverFileKey = rsa.EncryptDecrypt.encrypt(fileKey, eReceiver, nReceiver);
                System.out.println("STEP 3: SUCCESS - File key encrypted for receiver");
            } catch (Exception ex) {
                throw new RuntimeException("Failed to encrypt the file key for receiver: " + ex.getMessage(), ex);
            }

            Transaction transaction = new Transaction(sender, receiver, fileName, fileHash, sendersPublicKey, receiverPublicKey, eReceiverFileKey.toString(), type, null, null);

            // STEP 3: SIGN THE TRANSACTION WITH SENDER'S PRIVATE KEY
            try {
                String transactionString = new Gson().toJson(transaction);
                BigInteger transactionBigInt = new BigInteger(transactionString.getBytes(StandardCharsets.UTF_8));

                // Check if transactionBigInt is less than nSender, if it is then we use modulus
                if (transactionBigInt.compareTo(nSender) >= 0) {
                    transactionBigInt = transactionBigInt.mod(nSender);
                }

                String creatorSignature = EncryptDecrypt.sign(transactionBigInt, dSender, nSender);
                transaction.setCreatorSign(creatorSignature);

                System.out.println("STEP 4: SUCCESS - Transaction signed with sender's private key");

            } catch (Exception ex) {
                throw new RuntimeException("Failed to sign the transaction: " + ex.getMessage(), ex);
            }
            return transaction;
        }

        return null;
    }

    public static void main(String[] args) {
        if (args.length != 4 || args[0].startsWith("-help")) {
            System.out.println("use java -jar download.jar <fileName> <fileHash> <eFilekey> <sender> <receiver> <type>");
//            return;

            NodeInfo senderNode = new NodeInfo("user1", 8000, 0.9, 0.9);
            NodeInfo receiverNode = new NodeInfo("user2", 8001, 0.9, 0.9);

            args = new String[6];
            args[0] = "test.txt";
            args[1] = "Qmda7v4HnYEqbmCsVmGcxJ4SCtKRzNnRGaugXDGDzuN2F7";
            args[2] = "3381913732344884343920672375802526017904343425470814789163512246770566651028613508142597979720665049279148477073758936391731931681978291658904008769957571351711711224432331801817817165832523608040536351625118827313427601119258758934791754109155907207011239650287043220837215480382321863334034841229486243974996703823776287080858662269115934283222046659906228643593643453900569842039802015992707589812876505884979389720196140489570555177802300807505295166843260483627777604137734878279099483549081689750531415309484463382852778254105779000778450894195353850347361458887174567006268314397385917041026852140708126159404";
            args[3] = new Gson().toJson(senderNode);
            args[4] = new Gson().toJson(receiverNode);
            args[5] = "share";
        }

        // Parse sender and receiver to NodeInfo objects using Gson
         NodeInfo sender = new Gson().fromJson(args[3], NodeInfo.class);
         NodeInfo receiver = new Gson().fromJson(args[4], NodeInfo.class);

        // fileHash = Qmf5ztSr4jNjyzrQJpJ1QtyifdGRpF611mRsrK25VVY93P
        // eFilekey = 394057147557273929235744935992137414902850444887287784494046217507868918304590670940618394302293680949682110510100942970902052483889599409331407246571091971383578054338681009618919528605997466407836209605003903280153500239669374833956721866871658317218132623277218575404185888020459241066716079241005125857862743883180608573761960700981094636219405710238690787282847108819840689759192859406924353779430525275969238126212159337285309682784874666838451638029270942931004251097039973884519687495196539085716405948945107396893626799322495040440300748949748345019945199463188479887122592713074249836565420259756510133700

        String fileName = args[0];
        String fileHash = args[1];
        String eFilekey = args[2];
        String type = args[5];


        try {
            Transaction transaction = download(fileName, fileHash, eFilekey, sender, receiver, type);
            if (transaction != null) {
                System.out.println("Transaction created: " + transaction);
            } else {
                System.out.println("No transaction created.");
            }
        } catch (Exception e) {
            System.err.println("Error during download: " + e.getMessage());
        }

    }

}
