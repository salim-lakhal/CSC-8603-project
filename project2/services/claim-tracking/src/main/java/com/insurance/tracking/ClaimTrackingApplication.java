package com.insurance.tracking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// GraphQL service — GraphiQL available at http://localhost:8090/graphiql
@SpringBootApplication
public class ClaimTrackingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimTrackingApplication.class, args);
    }
}
