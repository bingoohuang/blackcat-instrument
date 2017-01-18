package com.github.bingoohuang.blackcat.javaagent.callback;

import lombok.Data;

@Data
public class BlackcatContext {
    private String traceId;
    private String parentLinkId;
    private int subLinkId = 0;

    public int incrAndGetSubLinkId() {
        return ++subLinkId;
    }
}
