package com.github.bingoohuang.blackcat.instrument.spring;

import com.github.bingoohuang.blackcat.instrument.callback.Blackcat;
import com.github.bingoohuang.blackcat.instrument.utils.BlackcatConfig;
import lombok.val;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class BlackcatLoggingFilter extends OncePerRequestFilter {
    private AtomicLong id = new AtomicLong(1);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, final FilterChain filterChain
    ) throws ServletException, IOException {
        if (!BlackcatConfig.isBlackcatSwitchOn()) {
            filterChain.doFilter(request, response);
            return;
        }

        val requestId = id.incrementAndGet();
        val requestWrapper = new BlackcatRequestWrapper(requestId, request);
        val responseWrapper = new BlackcatResponseWrapper(requestId, response);

        val startMillis = System.currentTimeMillis();
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            if (Blackcat.currentTraceId() == null) return;

            val endMillis = System.currentTimeMillis();
            requestWrapper.logRequest(startMillis);
            responseWrapper.logResponse(endMillis, endMillis - startMillis);
        }
    }
}