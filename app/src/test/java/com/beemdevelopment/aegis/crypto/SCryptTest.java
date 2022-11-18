package com.beemdevelopment.aegis.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

import com.beemdevelopment.aegis.crypto.bc.SCrypt;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.encoding.Hex;

import org.junit.Test;

import java.util.Arrays;

import javax.crypto.SecretKey;

public class SCryptTest {
    private static class Vector {
        private final byte[] _key;
        private final char[] _password;
        private final byte[] _salt;
        private final int _n;
        private final int _r;
        private final int _p;
        private final int _len;

        public Vector(String key, String password, String salt, int n, int r, int p, int len) throws EncodingException {
            _key = Hex.decode(key);
            _password = password.toCharArray();
            _salt = CryptoUtils.toBytes(salt.toCharArray());
            _n = n;
            _r = r;
            _p = p;
            _len = len;
        }

        public void validate() {
            SCryptParameters params = new SCryptParameters(_n, _r, _p, _salt);
            byte[] key = SCrypt.generate(CryptoUtils.toBytes(_password), params.getSalt(), params.getN(), params.getR(), params.getP(), _len);
            assertArrayEquals(_key, key);
        }
    }

    @Test
    public void vectorsMatch() throws EncodingException {
        // https://tools.ietf.org/html/rfc7914.html#section-12
        final Vector[] vectors = new Vector[]{
                new Vector("77d6576238657b203b19ca42c18a0497f16b4844e3074ae8dfdffa3fede21442fcd0069ded0948f8326a753a0fc81f17e8d3e0fb2e0d3628cf35e20c38d18906",
                        "", "", 1 << 4, 1, 1, 64),
                new Vector("fdbabe1c9d3472007856e7190d01e9fe7c6ad7cbc8237830e77376634b3731622eaf30d92e22a3886ff109279d9830dac727afb94a83ee6d8360cbdfa2cc0640",
                        "password", "NaCl", 1 << 10, 8, 16, 64),
                new Vector("7023bdcb3afd7348461c06cd81fd38ebfda8fbba904f8e3ea9b543f6545da1f2d5432955613f0fcf62d49705242a9af9e61e85dc0d651e40dfcf017b45575887",
                        "pleaseletmein", "SodiumChloride", 1 << 14, 8, 1, 64),
                new Vector("2101cb9b6a511aaeaddbbe09cf70f881ec568d574a2ffd4dabe5ee9820adaa478e56fd8f4ba5d09ffa1c6d927c40f4c337304049e8a952fbcbf45c6fa77a41a4",
                        "pleaseletmein", "SodiumChloride", 1 << 20, 8, 1, 64)
        };

        for (Vector vector : vectors) {
            vector.validate();
        }
    }

    @Test
    public void testTrailingNullCollision() throws EncodingException {
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
