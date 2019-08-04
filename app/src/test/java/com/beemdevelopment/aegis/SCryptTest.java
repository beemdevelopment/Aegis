package com.beemdevelopment.aegis;

import com.beemdevelopment.aegis.crypto.CryptoUtils;
import com.beemdevelopment.aegis.crypto.SCryptParameters;
import com.beemdevelopment.aegis.encoding.Hex;
import com.beemdevelopment.aegis.encoding.HexException;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

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

        byte[] head = new byte[]{'t', 'e', 's', 't'};
        byte[] expectedKey = Hex.decode("41cd8110d0c66ede16f97ce84fd8e2bd2269c9318532a01437789dfbadd1392e");

        for (int i = 0; i < 128; i += 4) {
            byte[] input = new byte[head.length + i];
            System.arraycopy(head, 0, input, 0, head.length);

            // once the length of the input is over 64 bytes, trailing nulls do not cause a collision anymore
            SecretKey key = CryptoUtils.deriveKey(input, params);
            if (input.length <= 64) {
                assertArrayEquals(expectedKey, key.getEncoded());
            } else {
                assertFalse(Arrays.equals(expectedKey, key.getEncoded()));
            }
        }
    }
}
