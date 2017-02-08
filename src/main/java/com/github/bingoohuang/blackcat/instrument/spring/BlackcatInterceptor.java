package com.github.bingoohuang.blackcat.instrument.spring;

import com.github.bingoohuang.blackcat.instrument.callback.Blackcat;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.alibaba.fastjson.JSON.toJSONString;

public class BlackcatInterceptor extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        Blackcat.reset(request);
        return super.preHandle(request, response, handler);
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
