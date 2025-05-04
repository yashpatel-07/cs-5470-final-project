package rsa;

import utils.WriteKeysUtil;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Random;

public class KeyGenerator {
    private static final Random rnd = new  Random();
    private static final BigInteger DEFAULT_E = BigInteger.valueOf(65537);

    public static KeyPair getRSAKeys() {
        int bitLength = 1024;

        BigInteger p = BigInteger.probablePrime(bitLength, rnd);
        BigInteger q;

        do {
            q = BigInteger.probablePrime(bitLength, rnd);
        } while (p.equals(q)); // Ensure p ≠ q

        // Calculate n = p * q
        BigInteger n = p.multiply(q);

        // Calculate ф(n) = (p-1) * (q-1)
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));

        // Find the relative e for phi
        BigInteger e = findRelativePrime(phi);

        // Calculate d, the inverse of e modulo ф(n)
        BigInteger d = e.modInverse(phi);

        return new KeyPair(new PublicKey(e, n), new PrivateKey(d, n));
    }

    public static BigInteger findRelativePrime(BigInteger phi) {
        if (DEFAULT_E.gcd(phi).equals(BigInteger.ONE)) {
            return DEFAULT_E;
        }

        BigInteger e = new BigInteger("2");

        while (e.gcd(phi).compareTo(BigInteger.ONE) != 0) {
            e = e.add(BigInteger.ONE);
        }

        return e;
    }

    public record PublicKey(BigInteger e, BigInteger n) implements Serializable {
    }

    public record PrivateKey(BigInteger d, BigInteger n) implements Serializable {
    }

    public record KeyPair(PublicKey publicKey, PrivateKey privateKey) implements Serializable {
    }

    public static void main(String[] args) {
        if (args.length != 1 || args[0].startsWith("-help")) {
            System.out.println("Usage:  java rsa.KeyGenerator genRSAKeys");
            return;
        }

        if (Objects.equals(args[0], "genRSAKeys")) {
            KeyGenerator.KeyPair user1KeyPair = KeyGenerator.getRSAKeys();
            KeyGenerator.KeyPair user2KeyPair = KeyGenerator.getRSAKeys();

            String user1Keys = "publicKey=" + user1KeyPair.publicKey.e() + "," + user1KeyPair.publicKey.n()
                    + "|privateKey=" + user1KeyPair.privateKey.d() + "," + user1KeyPair.privateKey.n();
            String user2Keys = "publicKey=" + user2KeyPair.publicKey.e() + "," + user2KeyPair.publicKey.n()
                    + "|privateKey=" + user2KeyPair.privateKey.d() + "," + user2KeyPair.privateKey.n();

            WriteKeysUtil.writeKeysToFile("RSA", "keys/user1.txt", user1Keys);
            WriteKeysUtil.writeKeysToFile("RSA", "keys/user2.txt", user2Keys);

            System.out.println("User1 keys: " + user1Keys);
            System.out.println("User2 keys: " + user2Keys);
            System.out.println("Keys generated and saved to files.");
        } else {
            System.out.println("Use command 'genRSAKeys' to generate keys.");
        }
    }
}
