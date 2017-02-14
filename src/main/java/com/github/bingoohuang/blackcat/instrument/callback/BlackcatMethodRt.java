package com.github.bingoohuang.blackcat.instrument.callback;

import lombok.Setter;
import lombok.ToString;
import org.n3r.idworker.Id;

import java.lang.management.ManagementFactory;

@ToString
public class BlackcatMethodRt {
    public final String invokeId = "" + Id.next();
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
    @Setter public String traceId;
    @Setter public String linkId;
    @Setter public String throwableMessage;

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
        this.throwableMessage = throwableCaught.getMessage();
        this.sameThrowable = throwableCaught == throwableUncaught;
    }

    public void setThrowableUncaught(Throwable throwableUncaught) {
        this.throwableUncaught = throwableUncaught;
        this.throwableMessage = throwableUncaught.getMessage();
        this.sameThrowable = throwableCaught == throwableUncaught;
    }

    public void finishExecute() {
        this.costNano = System.nanoTime() - startNano;
        this.endMillis = System.currentTimeMillis();
    }
}
