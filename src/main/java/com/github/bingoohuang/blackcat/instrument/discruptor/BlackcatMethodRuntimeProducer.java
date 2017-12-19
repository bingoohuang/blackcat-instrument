package com.github.bingoohuang.blackcat.instrument.discruptor;

import com.github.bingoohuang.blackcat.instrument.callback.BlackcatMethodRt;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMethodRuntime;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.lmax.disruptor.RingBuffer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import static com.alibaba.fastjson.JSON.toJSONString;

@Slf4j @AllArgsConstructor
public class BlackcatMethodRuntimeProducer {
    private final RingBuffer<BlackcatReq.Builder> ringBuffer;

    public void send(BlackcatMethodRt rt) {
        if (StringUtils.isBlank(rt.traceId)) return;

        val sequence = ringBuffer.next();  // Grab the next sequence
        try {
            val builder = ringBuffer.get(sequence); // Get the entry in the Disruptor
            // for the sequence Fill with data
            val head = Blackcats.buildHead(ReqType.BlackcatMethodRuntime);
            val rtBuilder = BlackcatMethodRuntime.newBuilder()
                    .setInvokeId(rt.invokeId)
                    .setTraceId(rt.traceId)
                    .setLinkId(rt.linkId)
                    .setPid(rt.pid)
                    .setExecutionId(rt.executionId)
                    .setStartMillis(rt.startMillis)
                    .setEndMillis(rt.endMillis)
                    .setCostNano(rt.costNano)

                    .setClassName(rt.className)
                    .setMethodName(rt.methodName)
                    .setMethodDesc(rt.methodDesc)
                    .setArgs(toJSONString(rt.args))
                    .setResult(toJSONString(rt.result))
                    .setThrowableCaught(toJSONString(rt.throwableCaught))
                    .setSameThrowable(rt.sameThrowable);
            if (rt.throwableMessage != null)
                rtBuilder.setThrowableMessage(rt.throwableMessage);
            if (!rt.sameThrowable) {
                rtBuilder.setThrowableUncaught(toJSONString(rt.throwableUncaught));
            }

            val methodRuntime = rtBuilder.build();
            builder.setBlackcatReqHead(head)
                    .setBlackcatMethodRuntime(methodRuntime);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

}
