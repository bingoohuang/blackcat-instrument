package com.github.bingoohuang.blackcat.instrument.spring;

import com.github.bingoohuang.blackcat.instrument.callback.Blackcat;
import com.github.bingoohuang.blackcat.instrument.callback.BlackcatContext;
import com.github.bingoohuang.blackcat.instrument.utils.MoreStr;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.alibaba.fastjson.JSON.toJSONString;

public class BlackcatInterceptor extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        val context = Blackcat.reset(request);

        appendTraceIdToCookie(request, response, context);
        response.addHeader(Blackcat.BLACKCAT_TRACE_ID, context.getTraceId());

        return super.preHandle(request, response, handler);
    }

    private void appendTraceIdToCookie(HttpServletRequest request, HttpServletResponse response, BlackcatContext
            context) {
        val traceIdCookie = findCookie(request);
        val traceIds = new StringBuilder();
        if (traceIdCookie != null) {
            traceIds.append(traceIdCookie.getValue());
        }
        if (traceIds.length() > 0) {
            traceIds.append(',').append(context.getTraceId());
        }

        int cutoffPos = MoreStr.lastOrdinalIndexOf(traceIds, ',', 30);
        if (cutoffPos > 0) {
            traceIds.delete(0, cutoffPos + 1);
        }

        response.addCookie(new Cookie(Blackcat.BLACKCAT_TRACE_ID, traceIds.toString()));
    }

    private Cookie findCookie(HttpServletRequest request) {
        val cookies = request.getCookies();
        for (val cookie : cookies) {
            if (cookie.getName().equals(Blackcat.BLACKCAT_TRACE_ID))
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
