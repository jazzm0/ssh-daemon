package com.sshdaemon.sshd;

import org.junit.Test;

import static com.sshdaemon.sshd.SshPassword.getRandomString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SshPasswordTest {

    @Test
    public void testCorrectLength() {
        assertEquals(10, getRandomString(10).length());
    }

    @Test
    public void testSecondDontEqual() {
        assertNotEquals(getRandomString(10), getRandomString(10));
    }
}