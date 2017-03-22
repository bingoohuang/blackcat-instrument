package com.github.bingoohuang.blackcat.instrument.spring;


import com.github.bingoohuang.blackcat.instrument.callback.Blackcat;
import com.github.bingoohuang.blackcat.instrument.utils.MoreStr;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.input.TeeInputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

@Slf4j
public class BlackcatRequestWrapper extends HttpServletRequestWrapper {
    private final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    private long requestId;


    public BlackcatRequestWrapper(Long requestId, HttpServletRequest request) {
        super(request);
        this.requestId = requestId;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

            private TeeInputStream tee = new TeeInputStream(BlackcatRequestWrapper.super.getInputStream(), bos);

            @Override
            public int read() throws IOException {
                return tee.read();
            }
        };
    }

    public byte[] toByteArray() {
        return bos.toByteArray();
    }


    @SneakyThrows
    public void logRequest(long startMillis) {
        val httpMethod = getMethod();
        val reqInfo = new StringBuilder("[Request-").append(requestId)
                .append("[").append(httpMethod).append(" ")
                .append(getRequestURL());
        String queryString = getQueryString();
        if (queryString != null) {
            reqInfo.append("?").append(queryString);
        }

        val reqMessage = new StringBuilder(reqInfo);
        reqMessage.append("] [HEAD:").append(createRequestHeads());
        reqMessage.append("] [PAYLOAD:").append(parsePayload());
        reqMessage.append("]");

        String reqLog = reqMessage.toString();
        Blackcat.trace("HTTP-REQ", reqLog);
    }

    private String parsePayload() {
        if (isMultipart() || isBinaryContent()) {
            return "Payload " + getContentType();
        }

        return MoreStr.getContent(toByteArray(), getCharacterEncoding());
    }

    private boolean isBinaryContent() {
        return MoreStr.startsWithAny(getContentType(), "image", "video", "audio");
    }

    private boolean isMultipart() {
        return MoreStr.startsWithAny(getContentType(), "multipart/form-data");
    }

    private String createRequestHeads() {
        val headerNames = getHeaderNames();
        val headerMap = new HashMap<String, String>();
        while (headerNames.hasMoreElements()) {
            val headerName = headerNames.nextElement();
            if (MoreStr.anyOf(headerName, "content-length", "accept-language",
                    "accept", "connection", "user-agent",
                    "blackcat-traceid", "accept-encoding", "dnt")) continue;

            val headers = getHeaders(headerName);
            val firstHeader = headers.nextElement();
            if (!headers.hasMoreElements()) {
                headerMap.put(headerName, firstHeader);
            } else {
                val headersString = new StringBuilder("[");
                headersString.append(firstHeader);
                while (headers.hasMoreElements()) {
                    headersString.append(",").append(headers.nextElement());
                }
                headersString.append("]");
                headerMap.put(headerName, headersString.toString());
            }
        }

        return headerMap.toString();
    }
}