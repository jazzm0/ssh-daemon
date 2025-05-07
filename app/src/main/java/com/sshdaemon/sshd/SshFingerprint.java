package com.sshdaemon.sshd;

import static java.util.Objects.isNull;

import org.apache.sshd.common.digest.BuiltinDigests;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.Map;

public class SshFingerprint {

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    private static final Map<Integer, String> CURVE_MAP = Map.of(
            256, "nistp256",
            384, "nistp384",
            521, "nistp521"
    );

    private static final int BYTE_SHIFT = 8;
    private static final int INT_SIZE = 4;

    private static String bytesToHex(byte[] bytes) {
        var hexChars = new char[bytes.length * 2];
        for (var j = 0; j < bytes.length; j++) {
            var v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static String getCurveName(int bitLength) {
        var curveName = CURVE_MAP.get(bitLength);
        if (isNull(curveName)) {
            throw new IllegalArgumentException("Unsupported ECDSA bit length: " + bitLength);
        }
        return curveName;
    }

    private static int getQLen(int bitLength) {
        if (bitLength <= 256) return 65;
        if (bitLength <= 384) return 97;
        if (bitLength <= 521) return 133;
        throw new IllegalArgumentException("Unsupported ECDSA bit length: " + bitLength);
    }

    private static void writeArray(final byte[] arr, final ByteArrayOutputStream baos) {
        for (var shift = (INT_SIZE - 1) * BYTE_SHIFT; shift >= 0; shift -= BYTE_SHIFT) {
            baos.write((arr.length >>> shift) & 0xFF);
        }
        baos.write(arr, 0, arr.length);
    }

    public static String fingerprintMD5(ECPublicKey publicKey) throws NoSuchAlgorithmException {
        return fingerprintMD5(encode(publicKey));
    }

    public static String fingerprintSHA256(ECPublicKey publicKey) throws NoSuchAlgorithmException {
        return fingerprintSHA256(encode(publicKey));
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
        var curveName = getCurveName(bitLength);
        var qLen = getQLen(bitLength);

        var name = ("ecdsa-sha2-" + curveName).getBytes(StandardCharsets.US_ASCII);
        var curve = curveName.getBytes(StandardCharsets.US_ASCII);
        writeArray(name, buf);
        writeArray(curve, buf);

        var javaEncoding = key.getEncoded();
        if (javaEncoding.length < qLen) {
            throw new IllegalArgumentException("Invalid key encoding length");
        }
        var q = new byte[qLen];
        System.arraycopy(javaEncoding, javaEncoding.length - qLen, q, 0, qLen);
        writeArray(q, buf);

        return buf.toByteArray();
    }

    public enum DIGESTS {
        MD5,
        SHA256
    }
}