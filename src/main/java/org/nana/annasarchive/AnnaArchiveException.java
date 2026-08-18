package org.nana.annasarchive;

public class AnnaArchiveException extends RuntimeException {

    public AnnaArchiveException(String message) {
        super(message);
    }

    public AnnaArchiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
