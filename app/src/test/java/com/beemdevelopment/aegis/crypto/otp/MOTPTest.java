package com.beemdevelopment.aegis.crypto.otp;

import static org.junit.Assert.assertEquals;

import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.encoding.Hex;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

public class MOTPTest {
    public static class Vector {
        public String Secret;
        public String OTP;
        public String Pin;
        public long Time;

        public Vector(long time, String otp, String pin, String secret) {
            Time = time;
            OTP = otp;
            Pin = pin;
            Secret = secret;
        }
    }

    public static final Vector[] VECTORS = {
            new Vector(165892298, "e7d8b6", "1234", "e3152afee62599c8"),
            new Vector(123456789, "4ebfb2", "1234", "e3152afee62599c8"),
            new Vector(165954002 * 10, "ced7b1", "9999", "bbb1912bb5c515be"),
            new Vector(165954002 * 10 + 2, "ced7b1", "9999", "bbb1912bb5c515be"),
            new Vector(165953987 * 10, "1a14f8", "9999", "bbb1912bb5c515be"),
            //should round down
            new Vector(165953987 * 10 + 8, "1a14f8", "9999", "bbb1912bb5c515be")
    };

    @Test
    public void testOutputCode() throws NoSuchAlgorithmException, EncodingException {
        for (Vector vector : VECTORS) {
            MOTP otp = MOTP.generateOTP(Hex.decode(vector.Secret), "MD5", 6, 10, vector.Pin, vector.Time);
            assertEquals(vector.OTP, otp.toString());
        }
    }

    @Test
    public void testGetDigest() throws NoSuchAlgorithmException {
        assertEquals("355938cfe3b73a624297591972d27c01",
                MOTP.getDigest("MD5", "BOB".getBytes(StandardCharsets.UTF_8)));
        assertEquals("16d7a4fca7442dda3ad93c9a726597e4",
                MOTP.getDigest("MD5", "test1234".getBytes(StandardCharsets.UTF_8)));
    }
}
