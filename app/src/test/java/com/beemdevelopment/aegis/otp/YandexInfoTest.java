package com.beemdevelopment.aegis.otp;

import static org.junit.Assert.assertThrows;

import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.EncodingException;

import org.junit.Test;

public class YandexInfoTest {
    private static final String[] vectors = new String[]{
            "LA2V6KMCGYMWWVEW64RNP3JA3IAAAAAAHTSG4HRZPI", // correct
            "LA2V6KMCGYMWWVEW64RNP3JA3I",                 // secret from QR - no validation
            "AA2V6KMCGYMWWVEW64RNP3JA3IAAAAAAHTSG4HRZPI", // first letter is different
            "AA2V6KMCGJA3IAAAAAAHTSG4HRZPI"               // size is wrong
    };

    @Test(expected = Test.None.class)
    public void testYandexSecretValidationOk() throws EncodingException, OtpInfoException {
        YandexInfo.validateSecret(getBase32Vector(0));
        YandexInfo.validateSecret(getBase32Vector(1));
    }

    @Test
    public void testYandexSecretValidation() {
        assertThrows(OtpInfoException.class, () -> YandexInfo.validateSecret(getBase32Vector(2)));
        assertThrows(OtpInfoException.class, () -> YandexInfo.validateSecret(getBase32Vector(3)));
    }

    private byte[] getBase32Vector(int vectorIndex) throws EncodingException {
        return Base32.decode(vectors[vectorIndex]);
    }
}
