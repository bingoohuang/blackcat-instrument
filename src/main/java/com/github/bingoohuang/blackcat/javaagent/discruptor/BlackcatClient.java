package com.github.bingoohuang.blackcat.javaagent.discruptor;

import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatMethodRt;
import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatMetricMsg;
import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatTraceMsg;
import com.github.bingoohuang.blackcat.sdk.netty.BlackcatNettyClient;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.lmax.disruptor.dsl.Disruptor;
import lombok.val;

import java.util.concurrent.Executors;

public class BlackcatClient {
    static BlackcatMethodRuntimeProducer blackcatMethodRtProducer;
    static BlackcatTraceMessageProducer traceMessageProducer;
    static BlackcatMetricProducer metricProducer;

    static {
        // Executor that will be used to construct new threads for consumers
        val executor = Executors.newSingleThreadExecutor();
        // The factory for the event
        val factory = new BlackcatReqFactory();
        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1024;
        // Construct the Disruptor
        val disruptor = new Disruptor<BlackcatReq.Builder>(factory, bufferSize, executor);

        val nettyClient = new BlackcatNettyClient();
        nettyClient.connect();

        // Connect the handler
        val handler = new BlackcatReqEventHandler(nettyClient);
        disruptor.handleEventsWith(handler);

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        val ringBuffer = disruptor.getRingBuffer();

        blackcatMethodRtProducer = new BlackcatMethodRuntimeProducer(ringBuffer);
        traceMessageProducer = new BlackcatTraceMessageProducer(ringBuffer);
        metricProducer = new BlackcatMetricProducer(ringBuffer);
    }

    public static void send(BlackcatMethodRt blackcatMethodRt) {
        blackcatMethodRtProducer.send(blackcatMethodRt);
    }

    public static void send(BlackcatTraceMsg traceMsg) {
        traceMessageProducer.send(traceMsg);
    }

    public static void send(BlackcatMetricMsg blackcatMetricMsg) {
        metricProducer.send(blackcatMetricMsg);
    }
}
