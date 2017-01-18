package com.github.bingoohuang.blackcat.javaagent.discruptor;

import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatMethodRt;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMethodRuntime;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.lmax.disruptor.RingBuffer;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import static com.alibaba.fastjson.JSON.toJSONString;

@Slf4j
public class BlackcatMethodRuntimeProducer {
    private final RingBuffer<BlackcatReq.Builder> ringBuffer;

    public BlackcatMethodRuntimeProducer(RingBuffer<BlackcatReq.Builder> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void send(BlackcatMethodRt blackcatMethodRt) {
        long sequence = ringBuffer.next();  // Grab the next sequence
        try {
            val builder = ringBuffer.get(sequence); // Get the entry in the Disruptor
            // for the sequence Fill with data
            val head = Blackcats.buildHead(ReqType.BlackcatMethodRuntime);
            val runtimeBuilder = BlackcatMethodRuntime.newBuilder()
                    .setInvokeId(blackcatMethodRt.invokeId)
                    .setPid(blackcatMethodRt.pid)
                    .setExecutionId(blackcatMethodRt.executionId)
                    .setStartMillis(blackcatMethodRt.startMillis)
                    .setEndMillis(blackcatMethodRt.endMillis)
                    .setCostNano(blackcatMethodRt.costNano)

                    .setClassName(blackcatMethodRt.className)
                    .setMethodName(blackcatMethodRt.methodName)
                    .setMethodDesc(blackcatMethodRt.methodDesc)
                    .setArgs(toJSONString(blackcatMethodRt.args))
                    .setResult(toJSONString(blackcatMethodRt.result))
                    .setThrowableCaught(toJSONString(blackcatMethodRt.throwableCaught))
                    .setSameThrowable(blackcatMethodRt.sameThrowable);
            if (!blackcatMethodRt.sameThrowable) {
                runtimeBuilder.setThrowableUncaught(
                        toJSONString(blackcatMethodRt.throwableUncaught));
            }

            val methodRuntime = runtimeBuilder.build();
            builder.setBlackcatReqHead(head)
                    .setBlackcatMethodRuntime(methodRuntime);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

}
