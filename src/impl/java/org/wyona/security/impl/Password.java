package org.wyona.security.impl;

import java.security.MessageDigest;

/**
 * Encrypt plain text password
 * Example: "message digest" becomes "f96b697d7cb7938d525a2f31aaf161d0" (hexadecimal notation (32 characters))
 */
public class Password {

    /**
     * Encrypt plain text password
     *
     * @param plain plain text password
     * @return encrypted password
     */
    public static String encrypt(String plain) {
        return getMD5(plain);
    }

    /**
     * Returns the MD5 representation of a string.
     * @param plain The plain string.
     * @return A string.
     */
    public static String getMD5(String plain) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return stringify(md.digest(plain.getBytes()));
    }

    /**
     * Converts a byte buffer to a string.
     * @param buf The buffer.
     * @return A string.
     */
    private static String stringify(byte[] buf) {
        StringBuffer sb = new StringBuffer(2 * buf.length);

        for (int i = 0; i < buf.length; i++) {
            int h = (buf[i] & 0xf0) >> 4;
            int l = (buf[i] & 0x0f);
            sb.append(new Character((char) ((h > 9) ? (('a' + h) - 10) : ('0' + h))));
            sb.append(new Character((char) ((l > 9) ? (('a' + l) - 10) : ('0' + l))));
        }

        return sb.toString();
    }
}