package com.github.bingoohuang.blackcat.instrument.annotations;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BlackcatMonitor {
    boolean debug() default false;
}
