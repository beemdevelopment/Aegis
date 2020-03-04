package com.beemdevelopment.aegis.importers;

public class DatabaseImporterEntryException extends Exception {
    private String _text;

    public DatabaseImporterEntryException(String message, String text) {
        super(message);
        _text = text;
    }

    public DatabaseImporterEntryException(Throwable cause, String text) {
        super(cause);
        _text = text;
    }

    public String getText() {
        return _text;
    }
}
