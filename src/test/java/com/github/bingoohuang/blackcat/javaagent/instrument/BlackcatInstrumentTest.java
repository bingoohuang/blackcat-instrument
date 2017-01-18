package com.github.bingoohuang.blackcat.javaagent.instrument;

import com.github.bingoohuang.blackcat.javaagent.utils.Tuple;
import com.google.common.io.Resources;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author bingoohuang [bingoohuang@gmail.com] Created on 2017/1/18.
 */
public class BlackcatInstrumentTest {
    @Test @SneakyThrows
    public void demo() {
        val url = Resources.getResource("Demo.class");
        byte[] bytes = Resources.toByteArray(url);
        val instrument = new BlackcatInstrument(bytes);
        Tuple<Boolean, byte[]> result = instrument.modifyClass();
        assertThat(result.x).isTrue();
    }
}
