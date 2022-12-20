package com.sshdaemon.sshd;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

public class SshFingerprint {

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

    public static String fingerprintMD5(byte[] keyBlob) throws NoSuchAlgorithmException {
        byte[] md5DigestPublic = MessageDigest.getInstance("MD5").digest(keyBlob);
        return bytesToHex(md5DigestPublic).replaceAll("(.{2})(?!$)", "$1:");
    }

    public static String fingerprintSHA256(byte[] keyBlob) throws NoSuchAlgorithmException {
        byte[] sha256DigestPublic = MessageDigest.getInstance("SHA-256").digest(keyBlob);
        return new String(Base64.getEncoder().encode(sha256DigestPublic));
    }

    public static byte[] encode(final ECPublicKey key) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        int bitLength = key.getW().getAffineX().bitLength();
        String curveName;
        int qLen;
        if (bitLength <= 256) {
            curveName = "nistp256";
            qLen = 65;
        } else if (bitLength <= 384) {
            curveName = "nistp384";
            qLen = 97;
        } else if (bitLength <= 521) {
            curveName = "nistp521";
            qLen = 133;
        } else {
            throw new RuntimeException("ECDSA bit length unsupported: " + bitLength);
        }

        byte[] name = ("ecdsa-sha2-" + curveName).getBytes(StandardCharsets.US_ASCII);
        byte[] curve = curveName.getBytes(StandardCharsets.US_ASCII);
        writeArray(name, buf);
        writeArray(curve, buf);

        byte[] javaEncoding = key.getEncoded();
        if (javaEncoding.length > 0) {
            byte[] q = new byte[qLen];

            System.arraycopy(javaEncoding, javaEncoding.length - qLen, q, 0, qLen);
            writeArray(q, buf);
        }
        return buf.toByteArray();
    }

    public static void writeArray(final byte[] arr, final ByteArrayOutputStream baos) {
        for (int shift = 24; shift >= 0; shift -= 8) {
            baos.write((arr.length >>> shift) & 0xFF);
        }
        baos.write(arr, 0, arr.length);
    }

    public enum DIGESTS {
        MD5,
        SHA256
    }
}
