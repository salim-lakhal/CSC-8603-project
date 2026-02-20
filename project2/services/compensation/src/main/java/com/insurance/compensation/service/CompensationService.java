package com.insurance.compensation.service;

import com.insurance.compensation.model.CompensationRequest;
import com.insurance.compensation.model.CompensationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CompensationService {

    private static final Logger log = LoggerFactory.getLogger(CompensationService.class);

    private static final double TAX_RATE = 0.02;

    /**
     * Formula:
     *   grossAmount  = approvedAmount
     *   netAmount    = approvedAmount - deductible
     *   taxAmount    = netAmount * 0.02
     *   totalPayment = netAmount - taxAmount
     */
    public CompensationResponse calculate(CompensationRequest request) {
        log.debug("Calculating compensation for claimId={}, approvedAmount={}, deductible={}",
                request.claimId(), request.approvedAmount(), request.deductible());

        if (request.deductible() > request.approvedAmount()) {
            throw new IllegalArgumentException(
                    "Deductible ($" + request.deductible()
                            + ") cannot exceed approved amount ($" + request.approvedAmount() + ")."
            );
        }

        double grossAmount = request.approvedAmount();
        double netAmount = grossAmount - request.deductible();
        double taxAmount = netAmount * TAX_RATE;
        double totalPayment = netAmount - taxAmount;

        log.info("Compensation calculated for claimId={}: gross={}, net={}, tax={}, total={}",
                request.claimId(), grossAmount, netAmount, taxAmount, totalPayment);

        return new CompensationResponse(
                request.claimId(),
                grossAmount,
                request.deductible(),
                netAmount,
                taxAmount,
                totalPayment
        );
    }
}
