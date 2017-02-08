package com.github.bingoohuang.blackcat.instrument.callback;

import com.github.bingoohuang.blackcat.instrument.discruptor.BlackcatClient;
import com.mashape.unirest.request.HttpRequest;
import lombok.val;
import org.n3r.idworker.Id;
import org.slf4j.MDC;
import org.slf4j.helpers.MessageFormatter;

import javax.servlet.http.HttpServletRequest;

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

        trace(blackcatContext.getSubLinkId(), msgType, msg);
        return blackcatContext;
    }

    public static BlackcatContext reset() {
        val context = threadLocal.get();
        if (context != null) return context;

        val traceId = String.valueOf(Id.next());
        val linkId = "0";
        return reset(traceId, linkId, "AUTO", "AUTO");
    }

    public static BlackcatContext reset(HttpServletRequest req) {
        String traceId = req.getHeader(BLACKCAT_TRACE_ID);
        if (isEmpty(traceId)) traceId = String.valueOf(Id.next());

        String linkId = req.getHeader(BLACKCAT_LINK_ID);
        if (isEmpty(linkId)) linkId = "0";

        val msg = req.getMethod() + ":" + getURL(req);
        return reset(traceId, linkId, "URL", msg);
    }

    public static void prepareRPC(HttpRequest httpRequest) {
        val context = threadLocal.get();
        if (context == null) return;

        val httpMethod = httpRequest.getHttpMethod().name();
        trace("RPC", httpMethod + ":" + httpRequest.getUrl());

        httpRequest.header(BLACKCAT_TRACE_ID, context.getTraceId());
        val linkId = context.getParentLinkId() + "." + context.getSubLinkId();
        httpRequest.header(BLACKCAT_LINK_ID, linkId);
    }

    public static void count(String metricName) {
        count(metricName, 1);
    }

    public static void count(String metricName, long countValue) {
        trace("COUNT", metricName + ":" + countValue);
        BlackcatClient.send(new BlackcatMetricMsg(metricName, countValue));
    }

    public static void sum(String metricName, long sumValue) {
        trace("SUM", metricName + ":" + sumValue);
        BlackcatClient.send(new BlackcatMetricMsg(metricName, sumValue));
    }

    public static void log(String pattern, Object... args) {
        trace("LOG", pattern, args);
    }

    public static String trace(String msgType, String pattern, Object... args) {
        val blackcatContext = reset();

        val msg = MessageFormatter.arrayFormat(pattern, args).getMessage();
        return trace(blackcatContext.incrAndGetSubLinkId(), msgType, msg);
    }

    public static String trace(int subLinkId, String msgType, String msg) {
        val blackcatContext = reset();

        val parentLinkId = blackcatContext.getParentLinkId();
        val traceId = blackcatContext.getTraceId();
        val linkId = parentLinkId + "." + subLinkId;

        val traceMsg = new BlackcatTraceMsg(traceId, linkId, msgType, msg);
        BlackcatClient.send(traceMsg);
        return linkId;
    }

    private BlackcatMethodRt rt;

    public String start(Object... params) {
        val stackTrace = Thread.currentThread().getStackTrace();

        int i = 0;
        for (int ii = stackTrace.length; i < ii; ++i) {
            if (stackTrace[i].getLineNumber() > 0) break;
        }

        val e = stackTrace[i + 2];
        val methodDesc = e.getFileName() + ":" + e.getLineNumber();

        return start(e.getClassName(), e.getMethodName(), methodDesc, params);
    }

    public String start(Class<?> clazz,
                        String methodName,
                        String methodDesc,
                        Object... params) {
        return start(clazz.getName(), methodName, methodDesc, params);
    }

    public String start(String className,
                        String methodName,
                        String methodDesc,
                        Object... params) {
        val instance = BlackcatJavaAgentCallback.getInstance();
        rt = instance.doStart(className, methodName, methodDesc, params);
        rt.setTraceId(currentTraceId());
        val linkId = trace("MethodStart",
                "Class:{},Method:{}, invokeId:{}",
                className, methodName, rt.invokeId);
        rt.setLinkId(linkId);
        return linkId;
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
