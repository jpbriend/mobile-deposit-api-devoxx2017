package com.cloudbees.example.mobile.deposit.api;
/**
 * Created by kmadel on 6/12/15.
 */

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;

@SpringBootApplication
public class MobileDepositApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MobileDepositApplication.class);
    }

    public static void main(String[] args) {
        new MobileDepositApplication().configure(
                new SpringApplicationBuilder(MobileDepositApplication.class)).run(args);
    }


}
