package com.sshdaemon.sshd;

import static com.sshdaemon.sshd.SshFingerprint.fingerprintMD5;
import static com.sshdaemon.sshd.SshFingerprint.fingerprintSHA256;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class SshFingerprintTest {

    private final byte[] decodedKey = Base64.getDecoder().decode("AAAAE2VjZHNhLXNoYTItbmlzdHA1MjEAAAAIbmlzdHA1MjEAAACFBAF9K9h1wxiYAH+FzBIAx9u3hJKaXbatDze1jSIT7YX748lPUko/04aABbMUd7SFWjmSUqYXz0ZaFuAbspTnl3fcaQAxaO2pm+8xGg41AxPAkcAa4hp2Mk6IqH2ctKfVPtZQHtB68Sy0+NsOYJmfSVWpKgMooVNYmOGVio6N7nfvfDS1IA==");

    @Test
    public void testMD5FingerPrint() throws NoSuchAlgorithmException {
        assertThat(fingerprintMD5(decodedKey),
                Matchers.is("27:64:65:20:26:7f:29:3d:07:66:c3:4c:65:eb:6f:b4"));
    }

    @Test
    public void testSHA256FingerPrint() throws NoSuchAlgorithmException {
        assertThat(fingerprintSHA256(decodedKey),
                Matchers.is("GzveilxzWuMhxIZRtkoVUelLOtm86qjJHKOb1Sr10+Q="));
    }
}