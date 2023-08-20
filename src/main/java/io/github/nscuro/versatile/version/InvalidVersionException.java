package io.github.nscuro.versatile.version;

import io.github.nscuro.versatile.VersException;

public class InvalidVersionException extends VersException {

    InvalidVersionException(final String message) {
        super(message);
    }

    InvalidVersionException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
