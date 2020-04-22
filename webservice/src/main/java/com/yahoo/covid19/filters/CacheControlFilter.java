/*
 * Copyright 2020, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.covid19.filters;

import com.yahoo.covid19.SecurityConfigProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * Turns on caching for 100% of API Requests for 1 hour.
 */
@Component
@Order(1)
@Slf4j
public class CacheControlFilter implements Filter {

    private static final String CACHE_CONTROL = "Cache-Control";
    private static final String DEFAULT_CACHE_SETTINGS = "no-cache, no-store";
    private SecurityConfigProperties properties;

    public CacheControlFilter(SecurityConfigProperties properties) {
        this.properties = properties;
    }

    @Override public void doFilter(ServletRequest request,
                                   ServletResponse response,
                                   FilterChain chain) throws IOException, ServletException {


        HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper((HttpServletResponse) response);
        if (properties.getCache().isEnabled()) {
            responseWrapper.addHeader(CACHE_CONTROL,
                    String.format("max-age=%d, s-maxage=%d, stale-while-revalidate=%d, stale-if-error=%d, public",
                            properties.getCache().getDuration(),
                            properties.getCache().getDuration(),
                            properties.getCache().getDuration(),
                            properties.getCache().getDuration()));
        } else {
            responseWrapper.addHeader(CACHE_CONTROL, DEFAULT_CACHE_SETTINGS);
        }
        chain.doFilter(request, responseWrapper);
    }
}
