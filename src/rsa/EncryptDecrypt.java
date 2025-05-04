package rsa;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class EncryptDecrypt {

    public static BigInteger encrypt(BigInteger plainText, BigInteger e, BigInteger n) {
        if (plainText.compareTo(n) >= 0) {
            throw new IllegalArgumentException("Plaintext must be smaller than n");
        }
        return plainText.modPow(e, n);
    }

    public static Object decrypt(BigInteger cipherText, BigInteger d, BigInteger n, Boolean retString) {
        BigInteger decryptedBigInt = cipherText.modPow(d, n);
        if (retString) {
           return new String(decryptedBigInt.toByteArray(), StandardCharsets.UTF_8);
        }
        return cipherText.modPow(d, n);
    }
}
