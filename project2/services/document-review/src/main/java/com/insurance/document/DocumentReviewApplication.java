package com.insurance.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// GraphQL service — GraphiQL available at http://localhost:8085/graphiql
@SpringBootApplication
public class DocumentReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentReviewApplication.class, args);
    }
}
