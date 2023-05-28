package com.beemdevelopment.aegis.otp;

import static org.junit.Assert.assertThrows;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.EncodingException;
import org.junit.Test;

public class YandexInfoTest {

    private static final String[] vectors = new String[] { // correct
    "LA2V6KMCGYMWWVEW64RNP3JA3IAAAAAAHTSG4HRZPI", // secret from QR - no validation
    "LA2V6KMCGYMWWVEW64RNP3JA3I", // first letter is different
    "AA2V6KMCGYMWWVEW64RNP3JA3IAAAAAAHTSG4HRZPI", // size is wrong
    "AA2V6KMCGJA3IAAAAAAHTSG4HRZPI" };

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
