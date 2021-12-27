package com.demo.springbootlogingintercepter.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.DelegatingServletOutputStream;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

@Component
@Slf4j
public class InternalLoggingUtils implements Filter {
    
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        MyCustomHttpRequestWrapper myCustomHttpRequestWrapper = new MyCustomHttpRequestWrapper((HttpServletRequest)servletRequest);
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("\n======================================== Internal Flow Started =================================================================================")
                        .append("\nRequest URI "+ myCustomHttpRequestWrapper.getRequestURI())
                                .append("\nRequest Method "+ myCustomHttpRequestWrapper.getMethod());
        String jsonParserRequest = null;
        try {
            jsonParserRequest = new JSONParser(new String(myCustomHttpRequestWrapper.getByteArray())).parse().toString();
        } catch (ParseException e) {
            log.error(e.getMessage());
        }
        requestBuilder.append("\nRequest Body "+jsonParserRequest);

        Enumeration<String> headerNames = myCustomHttpRequestWrapper.getHeaderNames();

        if (headerNames != null) {
            requestBuilder.append("\nRequest Headers:: {");
            while (headerNames.hasMoreElements()) {
                String s=headerNames.nextElement();
                requestBuilder.append(s+"  : "+myCustomHttpRequestWrapper.getHeader(s));
            }
            requestBuilder.append("}");
        }
        log.info(requestBuilder.toString());


        MyCustomHttpResponseWrapper myCustomHttpResponseWrapper = new MyCustomHttpResponseWrapper((HttpServletResponse)servletResponse);
        filterChain.doFilter(myCustomHttpRequestWrapper,myCustomHttpResponseWrapper);

        StringBuilder responseBuilder= new StringBuilder();
       responseBuilder.append("\nResponse Status "+ myCustomHttpResponseWrapper.getStatus());
        String jsonParserResponse = null;
        try {
            jsonParserResponse = new JSONParser(new String(myCustomHttpResponseWrapper.getBaos().toByteArray())).parse().toString();
        } catch (ParseException e) {
            log.error(e.getMessage());
        }
        responseBuilder.append("\nResponse Body "+jsonParserResponse);
        Collection<String> headers=myCustomHttpResponseWrapper.getHeaderNames();
        Iterator<String> iterator= headers.iterator();
        responseBuilder.append("\nResponse Headers:: {");
        while (iterator.hasNext()){
            String s=iterator.next();
            responseBuilder.append(s+"  : "+myCustomHttpResponseWrapper.getHeader(s));
        }
        responseBuilder.append("}")
        .append("\n=================================================== Internal Flow Ends ===========================================================================");

        log.info(responseBuilder.toString());

    }

    @Override
    public void destroy() {

    }

    private class MyCustomHttpRequestWrapper extends HttpServletRequestWrapper {

        private byte[] byteArray;

        public MyCustomHttpRequestWrapper(HttpServletRequest request) {
            super(request);

            try{
                byteArray = IOUtils.toByteArray(request.getInputStream());
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            DelegatingServletInputStream delegatingServletInputStream;
            return new DelegatingServletInputStream(new ByteArrayInputStream(byteArray));


        }

        public byte[] getByteArray() {
            return byteArray;
        }
    }

    private class MyCustomHttpResponseWrapper extends HttpServletResponseWrapper {
        private ByteArrayOutputStream baos = new ByteArrayOutputStream();
        private PrintStream printStream = new PrintStream(baos);

        public ByteArrayOutputStream getBaos() {
            return baos;
        }

        public MyCustomHttpResponseWrapper(HttpServletResponse servletResponse) {
            super(servletResponse);

        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return new DelegatingServletOutputStream( new TeeOutputStream(super.getOutputStream(), printStream));
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return new PrintWriter( new TeeOutputStream(super.getOutputStream(), printStream));
        }
    }
}

