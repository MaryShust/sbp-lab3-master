package com.example.sbp.delegate;

import com.example.sbp.exception.TransactionNotFoundException;
import com.example.sbp.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfirmPaymentDelegate implements JavaDelegate {

    private final PaymentService paymentService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("TEST");

        String transactionId = (String) execution.getVariable("transactionId");

        try {
            paymentService.confirmPaymentWithCheck(transactionId);
        } catch (TransactionNotFoundException e) {
            log.info("TEST 2");
        }
    }
}