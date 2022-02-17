package com.beemdevelopment.aegis.crypto.pins;

import info.guardianproject.trustedintents.ApkSignaturePin;

public class DebugSignaturePin extends ApkSignaturePin {
    public DebugSignaturePin(byte[] cert) {
        certificates = new byte[][] { cert };
        fingerprints = new String[] { getSHA256Fingerprint() };
    }
}
