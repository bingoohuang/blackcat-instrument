package com.github.bingoohuang.blackcat.javaagent.callback;

import lombok.Setter;
import lombok.ToString;

import java.lang.management.ManagementFactory;
import java.util.UUID;

@ToString
public class BlackcatMethodRt {
    public final String invokeId = UUID.randomUUID().toString();
    public final String executionId;
    public final String pid = getPid();
    public final long startMillis = System.currentTimeMillis();
    public final long startNano = System.nanoTime();
    public long endMillis;
    public long costNano;

    public final String className;
    public final String methodName;
    public final String methodDesc;
    public final Object[] args;
    public Throwable throwableCaught;
    @Setter public Object result;
    public Throwable throwableUncaught;
    public boolean sameThrowable = false;

    public static String getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0]; // --> 742912@localhost
    }

    public BlackcatMethodRt(
            String executionId,
            String className,
            String methodName,
            String methodDesc,
            Object[] args) {
        this.executionId = executionId;
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.args = args;
    }

    public void setThrowableCaught(Throwable throwableCaught) {
        this.throwableCaught = throwableCaught;
        this.sameThrowable = throwableCaught == throwableUncaught;
    }

    public void setThrowableUncaught(Throwable throwableUncaught) {
        this.throwableUncaught = throwableUncaught;
        this.sameThrowable = throwableCaught == throwableUncaught;
    }

    public void finishExecute() {
        this.costNano = System.nanoTime() - startNano;
        this.endMillis = System.currentTimeMillis();
    }
}
