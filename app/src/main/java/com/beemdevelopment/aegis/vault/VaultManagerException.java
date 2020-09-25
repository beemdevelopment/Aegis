package com.beemdevelopment.aegis.vault;

public class VaultManagerException extends Exception {
    public VaultManagerException(Throwable cause) {
        super(cause);
    }

    public VaultManagerException(String message) {
        super(message);
    }
}
