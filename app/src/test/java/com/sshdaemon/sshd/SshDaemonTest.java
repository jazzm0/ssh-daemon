package com.sshdaemon.sshd;

import static com.sshdaemon.sshd.SshDaemon.getFingerPrints;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class SshDaemonTest {

    @Test
    public void testLoadKeys() throws NoSuchAlgorithmException, IOException {
        Map<SshFingerprint.DIGESTS, String> fingerPrints = getFingerPrints();
        assertThat(fingerPrints.containsKey(SshFingerprint.DIGESTS.MD5), Matchers.is(true));
        assertThat(fingerPrints.containsKey(SshFingerprint.DIGESTS.SHA256), Matchers.is(true));
        assertThat(fingerPrints.get(SshFingerprint.DIGESTS.MD5), is(not(nullValue())));
        assertThat(fingerPrints.get(SshFingerprint.DIGESTS.SHA256), is(not(nullValue())));
    }
}