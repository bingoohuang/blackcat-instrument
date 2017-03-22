package com.github.bingoohuang.blackcat.instrument.spring;

import com.github.bingoohuang.blackcat.instrument.callback.Blackcat;
import com.github.bingoohuang.blackcat.instrument.utils.MoreStr;
import lombok.val;
import org.apache.commons.io.output.TeeOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class BlackcatResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    private PrintWriter writer = new PrintWriter(bos);
    private long requestId;

    public BlackcatResponseWrapper(Long requestId, HttpServletResponse response) {
        super(response);
        this.requestId = requestId;
    }

    @Override
    public ServletResponse getResponse() {
        return this;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {

            }

            private TeeOutputStream tee = new TeeOutputStream(BlackcatResponseWrapper.super.getOutputStream(), bos);

            @Override
            public void write(int b) throws IOException {
                tee.write(b);
            }
        };
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new TeePrintWriter(super.getWriter(), writer);
    }

    public byte[] toByteArray() {
        return bos.toByteArray();
    }

    public String getResponseBody() {
        byte[] toLog = toByteArray();
        if (toLog == null || toLog.length == 0) return "";

        return MoreStr.getContent(toLog, getCharacterEncoding());
    }

    public void logResponse(long endMillis, long costMillis) {
        val rspMessage = new StringBuilder("[Response-").append(requestId)
                .append("] [COST:").append(costMillis).append("millis")
                .append("] [Status:").append(getStatus())
                .append("] [HEAD:").append(createResponseHeads())
                .append("] [RSP BODY:").append(getResponseBody())
                .append("]");
        Blackcat.trace("HTTP-RSP", rspMessage.toString());
    }

    private String createResponseHeads() {
        val headerNames = getHeaderNames();
        val headerMap = new HashMap<String, String>();
        for (String headerName : headerNames) {
            if (MoreStr.anyOf(headerName, "Connection", "Date")) continue;

            val headers = getHeaders(headerName);
            if (headers.size() == 1) {
                headerMap.put(headerName, headers.iterator().next());
                continue;
            }

            val headersString = new StringBuilder("[");
            for (val header : headers) {
                headersString.append(header).append(",");
            }
            headersString.deleteCharAt(headersString.length() - 1);
            headersString.append("]");

            headerMap.put(headerName, headersString.toString());
        }
        return headerMap.toString();
    }
}