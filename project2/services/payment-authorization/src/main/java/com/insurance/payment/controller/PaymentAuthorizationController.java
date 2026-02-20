package com.insurance.payment.controller;

import com.insurance.payment.model.PaymentAuthorizationRequest;
import com.insurance.payment.model.PaymentAuthorizationResponse;
import com.insurance.payment.service.PaymentAuthorizationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentAuthorizationController {

    private static final Logger log = LoggerFactory.getLogger(PaymentAuthorizationController.class);

    private final PaymentAuthorizationService paymentAuthorizationService;

    public PaymentAuthorizationController(PaymentAuthorizationService paymentAuthorizationService) {
        this.paymentAuthorizationService = paymentAuthorizationService;
    }

    @PostMapping("/authorize")
    public ResponseEntity<PaymentAuthorizationResponse> authorizePayment(
            @Valid @RequestBody PaymentAuthorizationRequest request) {

        log.info("Received payment authorization request for claimId={}", request.claimId());

        PaymentAuthorizationResponse response = paymentAuthorizationService.authorize(request);
        return ResponseEntity.ok(response);
    }
}
