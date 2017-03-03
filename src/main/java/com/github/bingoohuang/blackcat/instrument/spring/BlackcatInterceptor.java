package com.github.bingoohuang.blackcat.instrument.spring;

import com.github.bingoohuang.blackcat.instrument.callback.Blackcat;
import com.github.bingoohuang.blackcat.instrument.callback.BlackcatContext;
import com.github.bingoohuang.blackcat.instrument.utils.BlackcatConfig;
import lombok.val;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.alibaba.fastjson.JSON.toJSONString;
import static org.apache.commons.lang3.StringUtils.ordinalIndexOf;

public class BlackcatInterceptor extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (BlackcatConfig.isBlackcatSwitchOn()) {
            val context = Blackcat.reset(request);
            responseTraceIds(request, response, context);
        }

        return super.preHandle(request, response, handler);
    }

    private void responseTraceIds(HttpServletRequest request,
                                  HttpServletResponse response,
                                  BlackcatContext context) {
        responseTraceIdsCookie(request, response, context);

        response.addHeader(Blackcat.BLACKCAT_TRACEID, context.getTraceId());
    }

    private void responseTraceIdsCookie(HttpServletRequest request,
                                        HttpServletResponse response,
                                        BlackcatContext context) {
        val traceIdCookie = findCookie(request, Blackcat.BLACKCAT_TRACEID);
        val traceIds = keepMaxTraceIds(context, traceIdCookie);

        val cookie = new Cookie(Blackcat.BLACKCAT_TRACEID, traceIds);
        cookie.setPath("/");
        cookie.setMaxAge(-1);
        response.addCookie(cookie);
    }

    private String keepMaxTraceIds(BlackcatContext context, Cookie traceIdCookie) {
        val traceIds = new StringBuilder(context.getTraceId());
        String x = "X";
        if (traceIdCookie != null) {
            traceIds.append(x).append(traceIdCookie.getValue());
        }

        int cutoffPos = ordinalIndexOf(traceIds, x, 30);
        if (cutoffPos > 0) {
            traceIds.delete(cutoffPos, traceIds.length());
        }

        return traceIds.toString();
    }

    private Cookie findCookie(HttpServletRequest request, String cookieName) {
        val cookies = request.getCookies();
        if (cookies == null) return null;

        for (val cookie : cookies) {
            if (cookie.getName().equals(cookieName))
                return cookie;
        }
        return null;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) throws Exception {
        Blackcat.trace("PostHandle", "post handle");
        super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        if (ex != null) Blackcat.trace("Exception", toJSONString(ex));
        Blackcat.trace("Completion", "after completion");

        super.afterCompletion(request, response, handler, ex);
    }
}
