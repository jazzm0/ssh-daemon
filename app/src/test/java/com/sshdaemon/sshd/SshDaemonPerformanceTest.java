package com.sshdaemon.sshd;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.apache.sshd.common.compression.BuiltinCompressions.delayedZlib;
import static org.apache.sshd.common.compression.BuiltinCompressions.zlib;

import com.sshdaemon.util.SftpOutputStreamWithChannel;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SshDaemonPerformanceTest {

    private SshClient client;
    private ClientSession session;
    private SftpClient sftpClient;

    @Before
    public void setUp() throws Exception {
        client = SshClient.setUpDefaultClient();
        client.setCompressionFactories(List.of(zlib, delayedZlib));
        client.start();

        session = client.connect("user", "10.94.221.97", 8022)
                .verify(10, TimeUnit.SECONDS)
                .getSession();
        session.addPasswordIdentity("gux");
        session.auth().verify(10, TimeUnit.SECONDS);
        sftpClient = SftpClientFactory.instance().createSftpClient(session);
    }

    @After
    public void tearDown() throws Exception {
        if (sftpClient != null) {
            sftpClient.close();
        }
        if (session != null) {
            session.close();
        }
        if (client != null) {
            client.stop();
        }
    }

    @Test
    public void uploadLatency() throws Exception {

        for (int megabytes : Arrays.asList(1, 5, 10, 50, 100)) {
            long t0 = System.currentTimeMillis();
            try (SftpClient client = SftpClientFactory.instance().createSftpClient(session)) {
                try (OutputStream os = new BufferedOutputStream(
                        new SftpOutputStreamWithChannel(
                                client, 32768, "out.txt",
                                Arrays.asList(SftpClient.OpenMode.Write,
                                        SftpClient.OpenMode.Create,
                                        SftpClient.OpenMode.Truncate)),
                        32768)) {
                    byte[] bytes = "123456789abcdef\n".getBytes();
                    for (int i = 0; i < 1024 * 1024 * megabytes / bytes.length; i++) {
                        os.write(bytes);
                    }
                }
            }
            long t1 = System.currentTimeMillis();
            long uploadDuration = t1 - t0;
            System.out.println("Upload duration: " + uploadDuration + " ms for " + megabytes + " MB" + " speed: " + (megabytes * 1000 * 1024 / (uploadDuration)) + " kB/s");
        }
    }
}