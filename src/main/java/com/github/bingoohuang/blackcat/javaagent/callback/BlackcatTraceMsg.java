package com.github.bingoohuang.blackcat.javaagent.callback;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class BlackcatTraceMsg {
    private final String traceId;
    private final String linkId;
    private final String msgType;
    private final String msg;
}
