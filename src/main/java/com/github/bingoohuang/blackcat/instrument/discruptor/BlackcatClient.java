package com.github.bingoohuang.blackcat.instrument.discruptor;

import com.github.bingoohuang.blackcat.instrument.callback.BlackcatMethodRt;
import com.github.bingoohuang.blackcat.instrument.callback.BlackcatMetricMsg;
import com.github.bingoohuang.blackcat.instrument.callback.BlackcatTraceMsg;
import com.github.bingoohuang.blackcat.instrument.utils.BlackcatConfig;
import com.github.bingoohuang.blackcat.sdk.netty.BlackcatNettyClient;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.lmax.disruptor.dsl.Disruptor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.concurrent.Executors;

@Slf4j
public class BlackcatClient {
    static BlackcatMethodRuntimeProducer blackcatMethodRtProducer;
    static BlackcatTraceMessageProducer traceMessageProducer;
    static BlackcatMetricProducer metricProducer;

    static {
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("init BlackcatClient failed", e);
        }
    }

    private static void init() {
        // The factory for the event
        val factory = new BlackcatReqFactory();
        // Specify the size of the ring buffer, must be power of 2.
        val bufferSize = 1024;
        val threadFactory = Executors.defaultThreadFactory();
        val disruptor = new Disruptor<BlackcatReq.Builder>(factory, bufferSize, threadFactory);

        val nettyClient = new BlackcatNettyClient();
        nettyClient.connect();

        // Connect the handler
        val handler = new BlackcatReqEventHandler(nettyClient);
        disruptor.handleEventsWith(handler);
        disruptor.start(); // Start the Disruptor, starts all threads running

        // Get the ring buffer from the Disruptor to be used for publishing.
        val ringBuffer = disruptor.getRingBuffer();
        blackcatMethodRtProducer = new BlackcatMethodRuntimeProducer(ringBuffer);
        traceMessageProducer = new BlackcatTraceMessageProducer(ringBuffer);
        metricProducer = new BlackcatMetricProducer(ringBuffer);
    }

    public static void send(BlackcatMethodRt blackcatMethodRt) {
        if (!BlackcatConfig.isBlackcatSwitchOn()) return;

        blackcatMethodRtProducer.send(blackcatMethodRt);
    }

    public static void send(BlackcatTraceMsg traceMsg) {
        if (!BlackcatConfig.isBlackcatSwitchOn()) return;

        traceMessageProducer.send(traceMsg);
    }

    public static void send(BlackcatMetricMsg blackcatMetricMsg) {
        if (!BlackcatConfig.isBlackcatSwitchOn()) return;

        metricProducer.send(blackcatMetricMsg);
    }
}
