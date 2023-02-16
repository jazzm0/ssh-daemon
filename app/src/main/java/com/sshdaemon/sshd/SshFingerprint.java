package com.sshdaemon.sshd;

import org.apache.sshd.common.digest.BuiltinDigests;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

public class SshFingerprint {

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        var hexChars = new char[bytes.length * 2];
        for (var j = 0; j < bytes.length; j++) {
            var v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String fingerprintMD5(byte[] keyBlob) throws NoSuchAlgorithmException {
        var md5DigestPublic = MessageDigest.getInstance(BuiltinDigests.Constants.MD5).digest(keyBlob);
        return bytesToHex(md5DigestPublic).replaceAll("(.{2})(?!$)", "$1:");
    }

    public static String fingerprintSHA256(byte[] keyBlob) throws NoSuchAlgorithmException {
        var sha256DigestPublic = MessageDigest.getInstance(BuiltinDigests.Constants.SHA256).digest(keyBlob);
        return new String(Base64.getEncoder().encode(sha256DigestPublic));
    }

    public static byte[] encode(final ECPublicKey key) {
        var buf = new ByteArrayOutputStream();

        var bitLength = key.getW().getAffineX().bitLength();
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

        var name = ("ecdsa-sha2-" + curveName).getBytes(StandardCharsets.US_ASCII);
        var curve = curveName.getBytes(StandardCharsets.US_ASCII);
        writeArray(name, buf);
        writeArray(curve, buf);

        var javaEncoding = key.getEncoded();
        if (javaEncoding.length > 0) {
            var q = new byte[qLen];

            System.arraycopy(javaEncoding, javaEncoding.length - qLen, q, 0, qLen);
            writeArray(q, buf);
        }
        return buf.toByteArray();
    }

    public static void writeArray(final byte[] arr, final ByteArrayOutputStream baos) {
        for (var shift = 24; shift >= 0; shift -= 8) {
            baos.write((arr.length >>> shift) & 0xFF);
        }
        baos.write(arr, 0, arr.length);
    }

    public enum DIGESTS {
        MD5,
        SHA256
    }
}
