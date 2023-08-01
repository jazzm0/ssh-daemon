package com.sshdaemon.sshd;

import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.isNull;

import com.sshdaemon.util.AndroidLogger;

import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;

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
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;


public class SshPublicKeyAuthenticator implements PublickeyAuthenticator {

    private final Set<PublicKey> authorizedKeys = new HashSet<>();

    public SshPublicKeyAuthenticator() {
    }

    private static byte[] readElement(DataInput dataInputStream) throws IOException {
        var buffer = new byte[dataInputStream.readInt()];
        dataInputStream.readFully(buffer);
        return buffer;
    }

    protected static PublicKey readKey(String key) throws Exception {
        var decodedKey = Base64.getDecoder().decode((key.split(" ")[1]));
        var dataInputStream = new DataInputStream(new ByteArrayInputStream(decodedKey));
        var pubKeyFormat = new String(readElement(dataInputStream));
        if (pubKeyFormat.equals("ssh-rsa")) {
            var publicExponent = readElement(dataInputStream);
            var modulus = readElement(dataInputStream);

            var specification = new RSAPublicKeySpec(new BigInteger(modulus), new BigInteger(publicExponent));
            var keyFactory = KeyFactory.getInstance("RSA");

            return (RSAPublicKey) keyFactory.generatePublic(specification);

        } else if (pubKeyFormat.equals("ssh-ed25519")) {
            var publicKey = new Ed25519PublicKeyParameters(readElement(dataInputStream), 0);
            return new EdDSAPublicKey(new EdDSAPublicKeySpec(publicKey.getEncoded(), EdDSANamedCurveTable.ED_25519_CURVE_SPEC));
        }
        throw new UnknownPublicKeyFormatException(pubKeyFormat);
    }

    Set<PublicKey> getAuthorizedKeys() {
        return unmodifiableSet(authorizedKeys);
    }

    public boolean loadKeysFromPath(String authorizedKeysPath) {
        var file = new File(authorizedKeysPath);
        AndroidLogger.getLogger().debug("Try to add authorized key file " + file.getPath());
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String line;
            while (!isNull((line = bufferedReader.readLine()))) {
                var key = readKey(line);
                authorizedKeys.add(key);
                AndroidLogger.getLogger().debug("Added authorized key" + key.toString());
            }
        } catch (Exception e) {
            AndroidLogger.getLogger().debug("Could not read authorized key file " + file.getPath());
            return false;
        }
        return true;
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
