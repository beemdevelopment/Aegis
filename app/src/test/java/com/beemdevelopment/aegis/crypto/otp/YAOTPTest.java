package com.beemdevelopment.aegis.crypto.otp;

import static org.junit.Assert.assertEquals;

import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.YandexInfo;

import org.junit.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class YAOTPTest {

    private static final Vector[] TEST_CASES = new Vector[]{
            new Vector("5239", "6SB2IKNM6OBZPAVBVTOHDKS4FAAAAAAADFUTQMBTRY", 1641559648L, "umozdicq"),
            new Vector("7586", "LA2V6KMCGYMWWVEW64RNP3JA3IAAAAAAHTSG4HRZPI", 1581064020L, "oactmacq"),
            new Vector("7586", "LA2V6KMCGYMWWVEW64RNP3JA3IAAAAAAHTSG4HRZPI", 1581090810L, "wemdwrix"),
            new Vector("5210481216086702", "JBGSAU4G7IEZG6OY4UAXX62JU4AAAAAAHTSG4HXU3M", 1581091469L, "dfrpywob"),
            new Vector("5210481216086702", "JBGSAU4G7IEZG6OY4UAXX62JU4AAAAAAHTSG4HXU3M", 1581093059L, "vunyprpd"),
    };

    @Test
    public void validateYaOtp()
            throws InvalidKeyException, NoSuchAlgorithmException, IOException, OtpInfoException {
        for (Vector testCase : TEST_CASES) {
            byte[] secret = YandexInfo.parseSecret(Base32.decode(testCase.secret));
            YAOTP otp = YAOTP.generateOTP(
                    secret,
                    testCase.pin,
                    8,
                    "HmacSHA256",
                    30,
                    testCase.timestamp
            );
            assertEquals(testCase.expected, otp.toString());
        }
    }

    public static class Vector {
        public String pin;
        public String secret;
        public long timestamp;
        public String expected;

        public Vector(String pin, String secret, long timestamp, String expected) {
            this.pin = pin;
            this.secret = secret;
            this.timestamp = timestamp;
            this.expected = expected;
        }
    }
}
