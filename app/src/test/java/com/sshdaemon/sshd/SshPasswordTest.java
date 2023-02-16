package com.sshdaemon.sshd;

import static com.sshdaemon.sshd.SshPassword.getRandomString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

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