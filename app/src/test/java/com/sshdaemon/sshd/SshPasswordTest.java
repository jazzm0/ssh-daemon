package com.sshdaemon.sshd;

import static com.sshdaemon.sshd.SshPassword.getRandomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class SshPasswordTest {

    @Test
    void testCorrectLength() {
        assertEquals(10, getRandomString(10).length());
    }

    @Test
    void testSecondDontEqual() {
        assertNotEquals(getRandomString(10), getRandomString(10));
    }
}