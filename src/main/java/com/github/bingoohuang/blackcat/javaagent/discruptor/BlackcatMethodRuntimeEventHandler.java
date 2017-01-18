package com.github.bingoohuang.blackcat.javaagent.discruptor;

import com.github.bingoohuang.blackcat.sdk.netty.BlackcatNettyClient;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.lmax.disruptor.EventHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor @Slf4j
public class BlackcatMethodRuntimeEventHandler implements EventHandler<BlackcatReq.Builder> {
    private final BlackcatNettyClient blackcatNettyClient;

    @Override
    public void onEvent(
            BlackcatReq.Builder builder,
            long sequence,
            boolean endOfBatch) throws Exception {
        BlackcatReq blackcatReq = builder.build();
        log.debug("send black req:{}", blackcatReq);
        blackcatNettyClient.send(blackcatReq);
    }
}
