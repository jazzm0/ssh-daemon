package com.sshdaemon.sshd;

import static com.sshdaemon.sshd.SshDaemon.getFingerPrints;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Environment;

import org.apache.sshd.contrib.server.subsystem.sftp.SimpleAccessControlSftpEventListener;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.subsystem.SubsystemFactory;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Environment.class, SshServer.class})
public class SshDaemonTest {

    @Rule
    TemporaryFolder tempFolder = new TemporaryFolder();

    SshServer sshServer = mock(SshServer.class);

    @Captor
    ArgumentCaptor<List<? extends SubsystemFactory>> captor;

    @Before
    public void doSetup() throws IOException {
        PowerMockito.mockStatic(Environment.class);
        PowerMockito.mockStatic(SshServer.class);

        when(Environment.getExternalStorageDirectory()).thenReturn(tempFolder.newFolder());
        when(SshServer.setUpDefaultServer()).thenReturn(sshServer);
    }


    @Test
    public void testLoadKeys() {
        var fingerPrints = getFingerPrints();
        assertThat(fingerPrints.containsKey(SshFingerprint.DIGESTS.MD5), Matchers.is(true));
        assertThat(fingerPrints.containsKey(SshFingerprint.DIGESTS.SHA256), Matchers.is(true));
        assertThat(fingerPrints.get(SshFingerprint.DIGESTS.MD5), is(not(nullValue())));
        assertThat(fingerPrints.get(SshFingerprint.DIGESTS.SHA256), is(not(nullValue())));
    }

    @Test
    public void testReadOnlySubsystem() {
        new SshDaemon(22, "user", "password", true);

        verify(sshServer).setPort(22);
        verify(sshServer).setPasswordAuthenticator(new SshPasswordAuthenticator("user", "password"));
        verify(sshServer).setSubsystemFactories(captor.capture());
        var factories = captor.getValue();
        assertThat(factories.size(), is(1));
        assertThat(((SftpSubsystemFactory) factories.get(0)).getRegisteredListeners().size(), is(1));
        assertTrue(((SftpSubsystemFactory) factories.get(0)).getRegisteredListeners().contains(SimpleAccessControlSftpEventListener.READ_ONLY_ACCESSOR));
    }

    @Test
    public void testReadWriteSubsystem() {
        new SshDaemon(22, "user", "password", false);

        verify(sshServer).setPort(22);
        verify(sshServer).setPasswordAuthenticator(new SshPasswordAuthenticator("user", "password"));
        verify(sshServer).setSubsystemFactories(captor.capture());
        var factories = captor.getValue();
        assertThat(factories.size(), is(1));
        assertThat(((SftpSubsystemFactory) factories.get(0)).getRegisteredListeners().size(), is(0));
    }
}