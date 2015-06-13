package com.cloudbees.example.mobile.deposit.api;

/**
 * Created by kmadel on 6/12/15.
 */
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

@Component
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        register(SimpleCORSFilter.class);
        register(DepositEndpoint.class);
    }

}