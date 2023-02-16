package com.sshdaemon.sshd;

import java.security.SecureRandom;

public class SshPassword {

    private static final String ALLOWED_CHARACTERS = "0123456789qwertzuiopasdfghjklyxcvbnm";

    public static String getRandomString(final int sizeOfPasswordString) {
        final var random = new SecureRandom();
        final var sb = new StringBuilder(sizeOfPasswordString);

        for (var i = 0; i < sizeOfPasswordString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));

        return sb.toString();
    }
}
