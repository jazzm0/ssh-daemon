package com.sshdaemon.sshd;

public class UnknownPublicKeyFormatException extends RuntimeException {
    public UnknownPublicKeyFormatException(String format) {
        super("Unknown public key format " + format);
    }
}
