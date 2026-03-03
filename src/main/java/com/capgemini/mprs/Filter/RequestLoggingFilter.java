package com.capgemini.mprs.Filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import jakarta.servlet.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

     private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Log the incoming request
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        logger.info(
                "Endpoint accessed: {} {}",
                httpRequest.getMethod(),
                httpRequest.getRequestURI()
        );
        filterChain.doFilter(request, response);
    }


}
