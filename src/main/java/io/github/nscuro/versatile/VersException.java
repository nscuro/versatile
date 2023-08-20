package io.github.nscuro.versatile;

public class VersException extends RuntimeException {

    protected VersException(final String message) {
        this(message, null);
    }

    protected VersException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
