package com.beemdevelopment.aegis.db.slots;

public class SlotException extends Exception {
    public SlotException(Throwable cause) {
        super(cause);
    }

    public SlotException(String message) {
        super(message);
    }
}
