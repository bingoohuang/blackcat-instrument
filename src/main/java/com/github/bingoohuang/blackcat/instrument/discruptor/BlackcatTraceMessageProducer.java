package com.github.bingoohuang.blackcat.instrument.discruptor;

import com.github.bingoohuang.blackcat.instrument.callback.BlackcatTraceMsg;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatTrace;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.lmax.disruptor.RingBuffer;
import lombok.AllArgsConstructor;
import lombok.val;

@AllArgsConstructor
public class BlackcatTraceMessageProducer {
    private final RingBuffer<BlackcatReq.Builder> ringBuffer;

    public void send(BlackcatTraceMsg traceMsg) {
        long sequence = ringBuffer.next();  // Grab the next sequence
        try {
            val builder = ringBuffer.get(sequence); // Get the entry in the Disruptor
            // for the sequence Fill with data
            val head = Blackcats.buildHead(ReqType.BlackcatTrace);
            val traceBuilder = BlackcatTrace.newBuilder()
                    .setTraceId(traceMsg.getTraceId())
                    .setLinkId(traceMsg.getLinkId())
                    .setMsgType(traceMsg.getMsgType())
                    .setMsg(traceMsg.getMsg());

            builder.setBlackcatReqHead(head)
                    .setBlackcatTrace(traceBuilder);
        } finally {
            ringBuffer.publish(sequence);
        }
    }
}
