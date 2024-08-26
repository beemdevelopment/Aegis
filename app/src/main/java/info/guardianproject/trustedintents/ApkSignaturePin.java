
package info.guardianproject.trustedintents;

import android.content.pm.Signature;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public abstract class ApkSignaturePin {

    protected String[] fingerprints; // hex-encoded SHA-256 hashes of the certs
    protected byte[][] certificates; // array of DER-encoded X.509 certificates
    private Signature[] signatures;

    public Signature[] getSignatures() {
        if (signatures == null) {
            signatures = new Signature[certificates.length];
            for (int i = 0; i < certificates.length; i++)
                signatures[i] = new Signature(certificates[i]);
        }
        return signatures;
    }

    /**
     * Gets the fingerprint of the first certificate in the signature.
     *
     * @param algorithm - Which hash to use (e.g. MD5, SHA1, SHA-256)
     * @return the fingerprint as hex String
     */
    public String getFingerprint(String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = md.digest(certificates[0]);
            BigInteger bi = new BigInteger(1, hashBytes);
            md.reset();
            return String.format("%0" + (hashBytes.length << 1) + "x", bi);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the MD5 fingerprint of the first certificate in the signature.
     *
     * @return the MD5 sum as hex String
     */
    public String getMD5Fingerprint() {
        return getFingerprint("MD5");
    }

    /**
     * Gets the SHA1 fingerprint of the first certificate in the signature.
     *
     * @return the SHA1 sum as hex String
     */
    public String getSHA1Fingerprint() {
        return getFingerprint("SHA1");
    }

    /**
     * Gets the SHA-256 fingerprint of the first certificate in the signature.
     *
     * @return the SHA-256 sum as hex String
     */
    public String getSHA256Fingerprint() {
        return getFingerprint("SHA-256");
    }

    /**
     * Compares the calculated SHA-256 cert fingerprint to the stored one.
     *
     * @return the result of the comparison
     */
    public boolean doFingerprintsMatchCertificates() {
        if (fingerprints == null || certificates == null)
            return false;
        String[] calcedFingerprints = new String[certificates.length];
        for (int i = 0; i < calcedFingerprints.length; i++)
            calcedFingerprints[i] = getSHA256Fingerprint();
        if (fingerprints.length == 0 || calcedFingerprints.length == 0)
            return false;
        return Arrays.equals(fingerprints, calcedFingerprints);
    }
}