package com.github.bingoohuang.blackcat.javaagent.discruptor;

import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatMethodRt;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMethodRuntime;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.lmax.disruptor.RingBuffer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import static com.alibaba.fastjson.JSON.toJSONString;

@Slf4j @AllArgsConstructor
public class BlackcatMethodRuntimeProducer {
    private final RingBuffer<BlackcatReq.Builder> ringBuffer;

    public void send(BlackcatMethodRt methodRt) {
        long sequence = ringBuffer.next();  // Grab the next sequence
        try {
            val builder = ringBuffer.get(sequence); // Get the entry in the Disruptor
            // for the sequence Fill with data
            val head = Blackcats.buildHead(ReqType.BlackcatMethodRuntime);
            val runtimeBuilder = BlackcatMethodRuntime.newBuilder()
                    .setInvokeId(methodRt.invokeId)
                    .setPid(methodRt.pid)
                    .setExecutionId(methodRt.executionId)
                    .setStartMillis(methodRt.startMillis)
                    .setEndMillis(methodRt.endMillis)
                    .setCostNano(methodRt.costNano)

                    .setClassName(methodRt.className)
                    .setMethodName(methodRt.methodName)
                    .setMethodDesc(methodRt.methodDesc)
                    .setArgs(toJSONString(methodRt.args))
                    .setResult(toJSONString(methodRt.result))
                    .setThrowableCaught(toJSONString(methodRt.throwableCaught))
                    .setSameThrowable(methodRt.sameThrowable);
            if (!methodRt.sameThrowable) {
                runtimeBuilder.setThrowableUncaught(
                        toJSONString(methodRt.throwableUncaught));
            }

            val methodRuntime = runtimeBuilder.build();
            builder.setBlackcatReqHead(head)
                    .setBlackcatMethodRuntime(methodRuntime);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

}
