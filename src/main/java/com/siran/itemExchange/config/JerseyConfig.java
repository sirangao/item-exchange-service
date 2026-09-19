package com.siran.itemExchange.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig(@Value("${app.cors.allowed-origin}") String allowedOrigin) {
        packages("com.siran.itemExchange.resource");

        register(new CorsFilter(allowedOrigin));
        register(ConstraintViolationMapper.class);
        register(WebApplicationExceptionMapper.class);
        register(DataIntegrityViolationMapper.class);
    }
}
