package com.sshdaemon.sshd;

import com.sshdaemon.util.AndroidLogger;

import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.isNull;
import static org.apache.commons.codec.binary.Base64.decodeBase64;


public class SshPublicKeyAuthenticator implements PublickeyAuthenticator {

    private final Set<RSAPublicKey> authorizedKeys = new HashSet<>();

    public SshPublicKeyAuthenticator() {
    }

    private static byte[] readElement(DataInput dataInputStream) throws IOException {
        byte[] buffer = new byte[dataInputStream.readInt()];
        dataInputStream.readFully(buffer);
        return buffer;
    }

    protected static RSAPublicKey readKey(String key) throws Exception {
        byte[] decodedKey = decodeBase64(key.split(" ")[1]);
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(decodedKey));
        String pubKeyFormat = new String(readElement(dataInputStream));
        if (!pubKeyFormat.equals("ssh-rsa"))
            throw new IllegalAccessException("Unsupported format");

        byte[] publicExponent = readElement(dataInputStream);
        byte[] modulus = readElement(dataInputStream);

        KeySpec specification = new RSAPublicKeySpec(new BigInteger(modulus), new BigInteger(publicExponent));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return (RSAPublicKey) keyFactory.generatePublic(specification);
    }

    public Set<RSAPublicKey> getAuthorizedKeys() {
        return unmodifiableSet(authorizedKeys);
    }

    public void loadKeysFromPath(String authorizedKeysPath) {
        File file = new File(authorizedKeysPath);
        AndroidLogger.getLogger().debug("Try to add authorized key file " + file.getPath());
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String line;
            while (!isNull((line = bufferedReader.readLine()))) {
                RSAPublicKey key = readKey(line);
                authorizedKeys.add(key);
                AndroidLogger.getLogger().debug("Added authorized key" + key.toString());
            }
        } catch (Exception e) {
            AndroidLogger.getLogger().debug("Could not read authorized key file " + file.getPath());
        }
    }

    @Override
    public boolean authenticate(String user, PublicKey publicKey, ServerSession serverSession) {
        if (authorizedKeys.contains(publicKey)) {
            AndroidLogger.getLogger().info("Successful public key authentication with key " + publicKey.toString() + " for user: " + user);
            return true;
        }
        return false;
    }
}
