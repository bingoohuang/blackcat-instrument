package com.github.bingoohuang.blackcat.instrument.callback;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value @AllArgsConstructor
public class BlackcatTraceMsg {
    private final String traceId;
    private final String linkId;
    private final String msgType;
    private final String msg;
}
