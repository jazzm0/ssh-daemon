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
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class SshPublicKeyAuthenticator implements PublickeyAuthenticator {

    private static final Logger LOGGER = AndroidLogger.getLogger();
    private static final String KEY_TYPE_RSA = "ssh-rsa";
    private static final String KEY_TYPE_ED25519 = "ssh-ed25519";
    private final Set<PublicKey> authorizedKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public SshPublicKeyAuthenticator() {
    }

    private static byte[] readElement(DataInputStream dataInput) throws IOException {
        int length = dataInput.readInt();
        if (length < 0 || length > 1024 * 1024) { // Prevent excessive allocation
            throw new IOException("Invalid element length: " + length);
        }
        byte[] buffer = new byte[length];
        dataInput.readFully(buffer);
        return buffer;
    }

    protected static PublicKey readKey(String key) throws Exception {
        if (isNull(key) || key.trim().isEmpty()) {
            LOGGER.error("Key string is empty or null");
            return null;
        }

        String[] parts = key.trim().split("\\s+");
        if (parts.length < 2) {
            LOGGER.error("Invalid key format: expected at least type and key");
            return null;
        }

        String keyType = parts[0];
        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(parts[1]);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid Base64 encoding in key", e);
            return null;
        }

        try (DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(decodedKey))) {
            String pubKeyFormat = new String(readElement(dataInputStream));
            if (!pubKeyFormat.equals(keyType)) {
                LOGGER.error("Key type mismatch: expected {}, got {}", keyType, pubKeyFormat);
                return null;
            }

            switch (pubKeyFormat) {
                case KEY_TYPE_RSA:
                    byte[] publicExponent = readElement(dataInputStream);
                    byte[] modulus = readElement(dataInputStream);
                    RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(modulus), new BigInteger(publicExponent));
                    KeyFactory rsaFactory = KeyFactory.getInstance("RSA");
                    return rsaFactory.generatePublic(spec);

                case KEY_TYPE_ED25519:
                    byte[] publicKeyBytes = readElement(dataInputStream);
                    Ed25519PublicKeyParameters params = new Ed25519PublicKeyParameters(publicKeyBytes, 0);
                    return new EdDSAPublicKey(new EdDSAPublicKeySpec(params.getEncoded(), EdDSANamedCurveTable.ED_25519_CURVE_SPEC));

                default:
                    LOGGER.error(pubKeyFormat);
                    return null;
            }
        }
    }

    Set<PublicKey> getAuthorizedKeys() {
        return unmodifiableSet(authorizedKeys);
    }

    public boolean loadKeysFromPath(String authorizedKeysPath) {
        authorizedKeys.clear();
        if (isNull(authorizedKeysPath)) {
            LOGGER.error("Authorized keys path is null");
            return false;
        }

        File file = new File(authorizedKeysPath);
        if (!file.exists() || !file.canRead()) {
            LOGGER.error("Authorized keys file {} does not exist or is not readable", authorizedKeysPath);
            return false;
        }

        LOGGER.debug("Loading authorized keys from {}", authorizedKeysPath);


        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                try {
                    PublicKey key = readKey(line);
                    if (isNull(key)) {
                        continue;
                    }
                    if (authorizedKeys.add(key)) {
                        LOGGER.debug("Added authorized key: type={}", key.getAlgorithm());
                    } else {
                        LOGGER.warn("Duplicate key ignored: {}", key.getAlgorithm());
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to parse key: {}", line, e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read authorized keys file {}", authorizedKeysPath, e);
            return false;
        }

        LOGGER.info("Loaded {} authorized keys from {}", authorizedKeys.size(), authorizedKeysPath);
        return !authorizedKeys.isEmpty();
    }

    @Override
    public boolean authenticate(String user, PublicKey publicKey, ServerSession serverSession) {
        if (isNull(publicKey)) {
            LOGGER.warn("Public key is null for user: {}", user);
            return false;
        }
        boolean authorized = authorizedKeys.contains(publicKey);
        LOGGER.info("Public key authentication {} for user: {}, key type: {}",
                authorized ? "succeeded" : "failed", user, publicKey.getAlgorithm());
        return authorized;
    }
}