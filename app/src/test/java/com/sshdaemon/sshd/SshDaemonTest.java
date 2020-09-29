package com.sshdaemon.sshd;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class SshDaemonTest {

    @Test
    public void testLoadKeys() throws NoSuchAlgorithmException {
        SshDaemon sshDaemon = new SshDaemon("/", 22, "user", "password");
        Map<SshFingerprint.DIGESTS, String> fingerPrints = sshDaemon.getFingerPrints();
        assertThat(fingerPrints.containsKey(SshFingerprint.DIGESTS.MD5), Matchers.is(true));
        assertThat(fingerPrints.containsKey(SshFingerprint.DIGESTS.SHA256), Matchers.is(true));
        assertThat(fingerPrints.get(SshFingerprint.DIGESTS.MD5), is(not(nullValue())));
        assertThat(fingerPrints.get(SshFingerprint.DIGESTS.SHA256), is(not(nullValue())));
    }
}