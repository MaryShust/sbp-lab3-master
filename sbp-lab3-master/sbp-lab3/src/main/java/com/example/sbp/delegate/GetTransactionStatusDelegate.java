package com.example.sbp.delegate;

import com.example.sbp.dto.PaymentResponseDTO;
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
public class GetTransactionStatusDelegate implements JavaDelegate {

    private final PaymentService paymentService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String transactionId = (String) execution.getVariable("transactionId");
        log.info("TEST 1");
        try {
            PaymentResponseDTO response = paymentService.getTransactionStatus(transactionId);
            execution.setVariable("transactionId", transactionId);
            execution.setVariable("status", response.getStatus());
            execution.setVariable("senderBillId", response.getSenderBillId());
            execution.setVariable("receiverBillId", response.getReceiverBillId());
            execution.setVariable("amount", response.getAmount().toString());
            execution.setVariable("commission", response.getCommission().toString());
            execution.setVariable("message", response.getMessage());
            log.info("TEST 2");
            execution.setVariable("statusFound", true);
        } catch (TransactionNotFoundException e) {
            log.info("TEST 3");
            execution.setVariable("bpmnError", "TRANSACTION_NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", "Транзакция не найдена: " + transactionId);
            execution.setVariable("statusFound", false);
        }
    }
}