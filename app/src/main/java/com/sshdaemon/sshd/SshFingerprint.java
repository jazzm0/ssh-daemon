package com.sshdaemon.sshd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class SshFingerprint {

    public enum DIGESTS {
        MD5,
        SHA256
    }

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String fingerprintMD5(BigInteger publicExponent, BigInteger modulus) throws NoSuchAlgorithmException {
        byte[] keyBlob = keyBlob(publicExponent, modulus);
        byte[] md5DigestPublic = MessageDigest.getInstance("MD5").digest(keyBlob);
        return bytesToHex(md5DigestPublic).replaceAll("(.{2})(?!$)", "$1:");
    }

    public static String fingerprintSHA256(BigInteger publicExponent, BigInteger modulus) throws NoSuchAlgorithmException {
        byte[] keyBlob = keyBlob(publicExponent, modulus);
        byte[] sha256DigestPublic = MessageDigest.getInstance("SHA-256").digest(keyBlob);
        return new String(Base64.getEncoder().encode(sha256DigestPublic));
    }

    private static byte[] keyBlob(BigInteger publicExponent, BigInteger modulus) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeLengthFirst("ssh-rsa".getBytes(), out);
            writeLengthFirst(publicExponent.toByteArray(), out);
            writeLengthFirst(modulus.toByteArray(), out);
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    // http://www.ietf.org/rfc/rfc4253.txt
    private static void writeLengthFirst(byte[] array, ByteArrayOutputStream out) throws IOException {
        out.write((array.length >>> 24) & 0xFF);
        out.write((array.length >>> 16) & 0xFF);
        out.write((array.length >>> 8) & 0xFF);
        out.write((array.length >>> 0) & 0xFF);
        if (array.length == 1 && array[0] == (byte) 0x00)
            out.write(new byte[0]);
        else
            out.write(array);
    }
}
