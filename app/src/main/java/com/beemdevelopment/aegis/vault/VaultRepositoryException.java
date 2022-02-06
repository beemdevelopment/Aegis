package com.beemdevelopment.aegis.vault;

public class VaultRepositoryException extends Exception {
    public VaultRepositoryException(Throwable cause) {
        super(cause);
    }

    public VaultRepositoryException(String message) {
        super(message);
    }
}
