package com.github.bingoohuang.blackcat.instrument.utils;

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
}
