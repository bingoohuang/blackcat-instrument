package com.github.bingoohuang.blackcat.instrument.utils;

import lombok.val;

import java.io.UnsupportedEncodingException;

/**
 * @author bingoohuang [bingoohuang@gmail.com] Created on 2017/2/16.
 */
public class MoreStr {

    public static int lastOrdinalIndexOf(CharSequence str, char searchChar, int ordinal) {
        for (int count = 0, i = str.length() - 1; i >= 0; --i) {
            if (str.charAt(i) != searchChar) continue;
            if (++count == ordinal) return i;
        }

        return -1;
    }

    public static int ordinalIndexOf(CharSequence str, char searchChar, int ordinal) {
        for (int count = 0, i = 0, ii = str.length(); i < ii; ++i) {
            if (str.charAt(i) != searchChar) continue;
            if (++count == ordinal) return i;
        }

        return -1;
    }

    public static boolean anyOf(String str, String... anys) {
        if (str == null) return false;
        for (String any : anys) {
            if (any.equals(str)) return true;
        }
        return false;
    }


    public static final int MAX_PAYLOAD_LENGTH = 1000;

    public static String getContent(byte[] buf, String charsetName) {
        if (buf == null || buf.length == 0) return "";
        int length = Math.min(buf.length, MAX_PAYLOAD_LENGTH);
        try {
            val encoding = charsetName != null ? charsetName : "ISO-8859-1";
            return new String(buf, 0, length, encoding);
        } catch (UnsupportedEncodingException ex) {
            return "Unsupported Encoding";
        }
    }

    public static boolean startsWithAny(String str, String... anys) {
        if (str == null) return false;

        for (val any : anys) {
            if (str.startsWith(any)) return true;
        }

        return false;
    }

}
