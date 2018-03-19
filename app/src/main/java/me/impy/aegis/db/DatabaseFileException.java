package me.impy.aegis.db;

public class DatabaseFileException extends Exception {
    public DatabaseFileException(Throwable cause) {
        super(cause);
    }

    public DatabaseFileException(String message) {
        super(message);
    }
}
