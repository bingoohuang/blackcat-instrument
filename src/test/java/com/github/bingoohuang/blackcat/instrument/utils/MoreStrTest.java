package com.github.bingoohuang.blackcat.instrument.utils;

import lombok.val;
import org.junit.Test;

import static com.github.bingoohuang.blackcat.instrument.utils.MoreStr.lastOrdinalIndexOf;
import static com.google.common.truth.Truth.assertThat;

/**
 * @author bingoohuang [bingoohuang@gmail.com] Created on 2017/2/16.
 */
public class MoreStrTest {
    @Test
    public void test1() {
        val s = "1,2,3,4,5";
        assertThat(lastOrdinalIndexOf(s, ',', 1)).isEqualTo(7);
        assertThat(lastOrdinalIndexOf(s, ',', 2)).isEqualTo(5);
        assertThat(lastOrdinalIndexOf(s, ',', 4)).isEqualTo(1);
        assertThat(lastOrdinalIndexOf(s, ',', 5)).isEqualTo(-1);
    }


}
