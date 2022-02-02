package com.beemdevelopment.aegis.encoding;

import java.io.IOException;

public class EncodingException extends IOException {
    public EncodingException(Throwable cause) {
        super(cause);
    }

    public EncodingException(String message) {
        super(message);
    }
}
