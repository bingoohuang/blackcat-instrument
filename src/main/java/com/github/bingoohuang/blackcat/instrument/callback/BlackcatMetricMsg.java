package com.github.bingoohuang.blackcat.instrument.callback;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class BlackcatMetricMsg {
    private final String metricName;
    private final long countValue;
}
