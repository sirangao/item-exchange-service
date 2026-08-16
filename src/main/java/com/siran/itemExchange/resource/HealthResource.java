package com.siran.itemExchange.resource;

import jakarta.annotation.Resource;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Path("/test")
public class HealthResource {
    @GET
    public String test() {
        return "Test successful";
    }

    @GET
    @Path("/user/{username}")
    public String getUsername(@PathParam("username") String username){
        return "Hello " + username;
    }



}
