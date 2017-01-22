package com.github.bingoohuang.blackcat.instrument.discruptor;

import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.lmax.disruptor.EventFactory;

public class BlackcatReqFactory implements EventFactory<BlackcatReq.Builder> {
    @Override
    public BlackcatReq.Builder newInstance() {
        return BlackcatReq.newBuilder();
    }
}
