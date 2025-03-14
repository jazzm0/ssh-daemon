package com.sshdaemon.sshd;

import static com.sshdaemon.sshd.SshFingerprint.fingerprintMD5;
import static com.sshdaemon.sshd.SshFingerprint.fingerprintSHA256;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

class SshFingerprintTest {

    private final byte[] decodedKey = Base64.getDecoder().decode("AAAAE2VjZHNhLXNoYTItbmlzdHA1MjEAAAAIbmlzdHA1MjEAAACFBAF9K9h1wxiYAH+FzBIAx9u3hJKaXbatDze1jSIT7YX748lPUko/04aABbMUd7SFWjmSUqYXz0ZaFuAbspTnl3fcaQAxaO2pm+8xGg41AxPAkcAa4hp2Mk6IqH2ctKfVPtZQHtB68Sy0+NsOYJmfSVWpKgMooVNYmOGVio6N7nfvfDS1IA==");
    private static ECPublicKey publicKey;

    @BeforeAll
    static void setUp() {
        var resourceDirectory = Paths.get("src", "test", "resources");
        var simpleGeneratorHostKeyProvider = new SimpleGeneratorHostKeyProvider(Paths.get(resourceDirectory.toFile().getAbsolutePath() + "/ssh_host_rsa_key"));
        var keyPairs = simpleGeneratorHostKeyProvider.loadKeys(null);
        publicKey = (ECPublicKey) keyPairs.get(0).getPublic();
    }

    @Test
    void testMD5FingerPrint() throws NoSuchAlgorithmException {
        assertThat(fingerprintMD5(decodedKey),
                Matchers.is("27:64:65:20:26:7f:29:3d:07:66:c3:4c:65:eb:6f:b4"));
    }

    @Test
    void testMD5FingerPrintPublicKey() throws NoSuchAlgorithmException {
        assertThat(fingerprintMD5(publicKey),
                Matchers.is("0c:78:6e:0c:06:41:79:c3:c6:08:75:cb:cf:81:20:d2"));
    }

    @Test
    void testSHA256FingerPrint() throws NoSuchAlgorithmException {
        assertThat(fingerprintSHA256(decodedKey),
                Matchers.is("GzveilxzWuMhxIZRtkoVUelLOtm86qjJHKOb1Sr10+Q="));
    }

    @Test
    void testSHA256FingerPrintPublicKey() throws NoSuchAlgorithmException {
        assertThat(fingerprintSHA256(publicKey),
                Matchers.is("74PJTvpjtpKF/+M25EdRMqwimWF9oX4w35EK6UEhefM="));
    }
}