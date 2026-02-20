package com.insurance.payment.service;

import com.insurance.payment.model.PaymentAuthorizationRequest;
import com.insurance.payment.model.PaymentAuthorizationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentAuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentAuthorizationService.class);

    /**
     * Authorization rules:
     *   totalPayment > 0 AND bankAccount not blank -> AUTHORIZED
     *   otherwise -> DENIED
     */
    public PaymentAuthorizationResponse authorize(PaymentAuthorizationRequest request) {
        log.debug("Processing payment authorization for claimId={}, totalPayment={}",
                request.claimId(), request.totalPayment());

        String authorizationId = UUID.randomUUID().toString();

        boolean isValidAmount = request.totalPayment() > 0;
        boolean isValidAccount = request.bankAccount() != null
                && !request.bankAccount().isBlank();

        if (isValidAmount && isValidAccount) {
            String authCode = generateAuthorizationCode(request.claimId());
            log.info("Payment AUTHORIZED for claimId={}, amount={}, authCode={}",
                    request.claimId(), request.totalPayment(), authCode);

            return new PaymentAuthorizationResponse(
                    authorizationId,
                    request.claimId(),
                    "AUTHORIZED",
                    authCode,
                    "Payment of $" + String.format("%.2f", request.totalPayment())
                            + " authorized for claim " + request.claimId()
                            + ". Funds will be transferred to account ending in "
                            + maskAccountNumber(request.bankAccount()) + "."
            );
        }

        String denialReason = buildDenialReason(isValidAmount, isValidAccount);
        log.warn("Payment DENIED for claimId={}: {}", request.claimId(), denialReason);

        return new PaymentAuthorizationResponse(
                authorizationId,
                request.claimId(),
                "DENIED",
                "N/A",
                denialReason
        );
    }

    private String generateAuthorizationCode(String claimId) {
        int hash = Math.abs(claimId.hashCode()) % 1_000_000;
        return String.format("AUTH-%06d", hash);
    }

    private String maskAccountNumber(String account) {
        if (account.length() <= 4) {
            return account;
        }
        return "*".repeat(account.length() - 4) + account.substring(account.length() - 4);
    }

    private String buildDenialReason(boolean isValidAmount, boolean isValidAccount) {
        if (!isValidAmount && !isValidAccount) {
            return "Payment denied: total payment amount must be greater than zero "
                    + "and a valid bank account must be provided.";
        }
        if (!isValidAmount) {
            return "Payment denied: total payment amount must be greater than zero.";
        }
        return "Payment denied: a valid bank account number must be provided.";
    }
}
