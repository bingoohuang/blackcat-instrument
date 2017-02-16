package com.github.bingoohuang.blackcat.instrument.discruptor;

import com.github.bingoohuang.blackcat.instrument.callback.BlackcatMetricMsg;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMetric;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.lmax.disruptor.RingBuffer;
import lombok.AllArgsConstructor;
import lombok.val;

@AllArgsConstructor
public class BlackcatMetricProducer {
    private final RingBuffer<BlackcatReq.Builder> ringBuffer;

    public void send(BlackcatMetricMsg metric) {
        val sequence = ringBuffer.next();  // Grab the next sequence
        try {
            val builder = ringBuffer.get(sequence); // Get the entry in the Disruptor
            // for the sequence Fill with data
            val head = Blackcats.buildHead(ReqType.BlackcatMetric);
            val metricBuilder = BlackcatMetric.newBuilder()
                    .setName(metric.getMetricName())
                    .setValue(metric.getCountValue());

            builder.setBlackcatReqHead(head)
                    .setBlackcatMetric(metricBuilder);
        } finally {
            ringBuffer.publish(sequence);
        }
    }
}
