package com.github.bingoohuang.blackcat.instrument.callback;

import com.github.bingoohuang.blackcat.instrument.discruptor.BlackcatClient;
import com.mashape.unirest.request.HttpRequest;
import lombok.val;
import org.slf4j.MDC;
import org.slf4j.helpers.MessageFormatter;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class Blackcat {
    public static final String BLACKCAT_TRACE_ID = "blackcat_trace_id";
    public static final String BLACKCAT_LINK_ID = "blackcat_link_id";

    static ThreadLocal<BlackcatContext> threadLocal = new InheritableThreadLocal<BlackcatContext>();

    public static BlackcatContext reset(
            String blackcatTraceId,
            String parentLinkId,
            String msgType,
            String msg) {
        val blackcatContext = new BlackcatContext();
        blackcatContext.setTraceId(blackcatTraceId);
        blackcatContext.setParentLinkId(parentLinkId);

        MDC.put(BLACKCAT_TRACE_ID, blackcatTraceId);
        threadLocal.set(blackcatContext);

        log(blackcatContext.getSubLinkId(), msgType, msg);
        return blackcatContext;
    }

    public static BlackcatContext reset() {
        val context = threadLocal.get();
        if (context != null) return context;

        val traceId = UUID.randomUUID().toString();
        val linkId = "0";
        return reset(traceId, linkId, "AUTO", "AUTO");
    }

    public static BlackcatContext reset(HttpServletRequest req) {
        String traceId = req.getHeader(BLACKCAT_TRACE_ID);
        if (isEmpty(traceId)) traceId = UUID.randomUUID().toString();

        String linkId = req.getHeader(BLACKCAT_LINK_ID);
        if (isEmpty(linkId)) linkId = "0";

        val msg = req.getMethod() + ":" + getURL(req);
        return reset(traceId, linkId, "URL", msg);
    }

    public static void prepareRPC(HttpRequest httpRequest) {
        val context = threadLocal.get();
        if (context == null) return;

        val httpMethod = httpRequest.getHttpMethod().name();
        log("RPC", httpMethod + ":" + httpRequest.getUrl());

        httpRequest.header(BLACKCAT_TRACE_ID, context.getTraceId());
        val linkId = context.getParentLinkId() + "." + context.getSubLinkId();
        httpRequest.header(BLACKCAT_LINK_ID, linkId);
    }

    public static void count(String metricName) {
        count(metricName, 1);
    }

    public static void count(String metricName, long countValue) {
        logMsg("COUNT", metricName + ":" + countValue);
        BlackcatClient.send(new BlackcatMetricMsg(metricName, countValue));
    }

    public static void sum(String metricName, long sumValue) {
        logMsg("SUM", metricName + ":" + sumValue);
        BlackcatClient.send(new BlackcatMetricMsg(metricName, sumValue));
    }

    public static void log(String pattern, Object... args) {
        logMsg("LOG", pattern, args);
    }

    public static void logMsg(String msgType, String pattern, Object... args) {
        val blackcatContext = reset();

        val msg = MessageFormatter.arrayFormat(pattern, args).getMessage();
        log(blackcatContext.incrAndGetSubLinkId(), msgType, msg);
    }

    public static void log(int subLinkId, String msgType, String msg) {
        val blackcatContext = reset();

        val parentLinkId = blackcatContext.getParentLinkId();

        val traceId = blackcatContext.getTraceId();
        val linkId = parentLinkId + "." + subLinkId;

        val traceMsg = new BlackcatTraceMsg(traceId, linkId, msgType, msg);
        BlackcatClient.send(traceMsg);
    }

    private BlackcatMethodRt rt;

    public void start(Object... params) {
        val stackTrace = Thread.currentThread().getStackTrace();

        int i = 0;
        for (int ii = stackTrace.length; i < ii; ++i) {
            if (stackTrace[i].getLineNumber() > 0) break;
        }

        StackTraceElement e = stackTrace[i + 2];
        val methodDesc = e.getFileName() + ":" + e.getLineNumber();

        start(e.getClassName(), e.getMethodName(), methodDesc, params);
    }

    public void start(Class<?> clazz,
                      String methodName,
                      String methodDesc,
                      Object... params) {
        start(clazz.getName(), methodName, methodDesc, params);
    }

    public void start(String className,
                      String methodName,
                      String methodDesc,
                      Object... params) {
        val instance = BlackcatJavaAgentCallback.getInstance();
        rt = instance.doStart(className, methodName, methodDesc, params);
        log("MethodStart", "invokeId:" + rt.invokeId
                + "@" + className + "." + methodName);
    }

    public void finish() {
        val instance = BlackcatJavaAgentCallback.getInstance();
        instance.doVoidFinish(rt);
    }

    public void finish(Object result) {
        val instance = BlackcatJavaAgentCallback.getInstance();
        instance.doFinish(rt, result);
    }

    public void uncaught(Throwable throwable) {
        val instance = BlackcatJavaAgentCallback.getInstance();
        instance.doThrowableUncaught(rt, throwable);
    }

    public void caught(Throwable throwable) {
        val instance = BlackcatJavaAgentCallback.getInstance();
        instance.doThrowableCaught(rt, throwable);
    }

    public static String getURL(HttpServletRequest req) {
        val scheme = req.getScheme();             // http
        val serverName = req.getServerName();     // hostname.com
        val serverPort = req.getServerPort();     // 80
        val contextPath = req.getContextPath();   // /mywebapp
        val servletPath = req.getServletPath();   // /servlet/MyServlet
        val pathInfo = req.getPathInfo();         // /a/b;c=123
        val queryString = req.getQueryString();   // d=789

        // Reconstruct original requesting URL
        val url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        if (serverPort != 80 && serverPort != 443) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath).append(servletPath);

        if (pathInfo != null) url.append(pathInfo);
        if (queryString != null) url.append("?").append(queryString);
        return url.toString();
    }

    public static String currentTraceId() {
        val context = threadLocal.get();
        if (context == null) return null;

        return context.getTraceId();
    }
}
