package com.github.bingoohuang.blackcat.instrument.utils;

import com.github.bingoohuang.westjson.WestJson;
import lombok.val;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author bingoohuang [bingoohuang@gmail.com] Created on 2017/2/16.
 */
public class Collections {
    public static String compressResult(Collection<Object> col) {
        val head = head(col, 10);
        return head.size() + "/" + col.size() + " " + toJson(head);
    }

    private static List<Object> head(Collection col, int size) {
        val list = new ArrayList<Object>(size);
        val iterator = col.iterator();
        for (int i = 0; i < 10; ++i) {
            if (!iterator.hasNext()) break;

            list.add(iterator.next());
        }
        return list;
    }

    private static String toJson(Object obj) {
        return new WestJson().json(obj, WestJson.UNQUOTED);
    }
}
