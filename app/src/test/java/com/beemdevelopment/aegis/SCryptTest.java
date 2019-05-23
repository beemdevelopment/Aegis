package com.beemdevelopment.aegis;

import com.beemdevelopment.aegis.crypto.CryptoUtils;
import com.beemdevelopment.aegis.crypto.SCryptParameters;
import com.beemdevelopment.aegis.encoding.Hex;
import com.beemdevelopment.aegis.encoding.HexException;

import org.junit.Test;

import javax.crypto.SecretKey;

import static org.junit.Assert.*;

public class SCryptTest {
    @Test
    public void testTrailingNullCollision() throws HexException {
        byte[] salt = new byte[0];
        SCryptParameters params = new SCryptParameters(
                CryptoUtils.CRYPTO_SCRYPT_N,
                CryptoUtils.CRYPTO_SCRYPT_p,
                CryptoUtils.CRYPTO_SCRYPT_r,
                salt
        );

        byte[] expectedKey = Hex.decode("41cd8110d0c66ede16f97ce84fd8e2bd2269c9318532a01437789dfbadd1392e");
        byte[][] inputs = new byte[][]{
                new byte[]{'t', 'e', 's', 't'},
                new byte[]{'t', 'e', 's', 't', '\0'},
                new byte[]{'t', 'e', 's', 't', '\0', '\0'},
                new byte[]{'t', 'e', 's', 't', '\0', '\0', '\0'},
                new byte[]{'t', 'e', 's', 't', '\0', '\0', '\0', '\0'},
                new byte[]{'t', 'e', 's', 't', '\0', '\0', '\0', '\0', '\0'},
        };

        for (byte[] input : inputs) {
            SecretKey key = CryptoUtils.deriveKey(input, params);
            assertArrayEquals(expectedKey, key.getEncoded());
        }
    }
}
