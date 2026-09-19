package com.siran.itemExchange.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;

/**
 * In development the React dev server proxies /api to this app, so requests are
 * same-origin and these headers are unused. They matter once the built frontend is
 * served from somewhere other than this host.
 */
public class CorsFilter implements ContainerResponseFilter {

    private final String allowedOrigin;

    public CorsFilter(String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        response.getHeaders().putSingle("Access-Control-Allow-Origin", allowedOrigin);
        response.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.getHeaders().putSingle("Access-Control-Allow-Headers", HttpHeaders.CONTENT_TYPE + ", " + HttpHeaders.AUTHORIZATION);
        response.getHeaders().putSingle("Access-Control-Max-Age", "3600");
    }
}
