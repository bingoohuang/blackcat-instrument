package com.github.bingoohuang.blackcat.instrument.utils;

import lombok.val;
import org.n3r.diamond.client.Miner;

/**
 * Created by huangjb on 2017/3/1.
 */
public class BlackcatConfig {
    public static boolean isBlackcatSwitchOn() {
        val switchConf = new Miner().getMiner("blackcatserver", "config");
        return "on".equals(switchConf.getString("switch"));
    }
}
