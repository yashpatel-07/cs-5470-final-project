package fernet;

import javax.crypto.SecretKey;

public class FernetKeyPair {
    public final SecretKey aesKey;
    public final SecretKey hmacKey;

    public FernetKeyPair(SecretKey aesKey, SecretKey hmacKey) {
        this.aesKey = aesKey;
        this.hmacKey = hmacKey;
    }
}
