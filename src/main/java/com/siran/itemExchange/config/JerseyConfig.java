package com.siran.itemExchange.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

import static org.springframework.core.annotation.AnnotationFilter.packages;

@Component
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        //register(HealthResource.class);
        packages("com.siran.itemExchange.resource");
    }
}