package com.github.bingoohuang.blackcat.instrument.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class Tuple<X, Y> {
    public final X x;
    public final Y y;
}