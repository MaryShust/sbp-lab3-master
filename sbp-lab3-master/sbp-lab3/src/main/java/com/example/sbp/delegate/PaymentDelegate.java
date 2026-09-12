package com.example.sbp.delegate;

import com.example.sbp.exception.BillNotFoundException;
import com.example.sbp.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentDelegate implements JavaDelegate {

    private final PaymentService paymentService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("TEST Checking if accounts are active");

        String senderBillId = (String) execution.getVariable("sender_bill_id");
        String receiverBillId = (String) execution.getVariable("receiverBillId");
        String senderBankBic = (String) execution.getVariable("senderBankBic");
        String receiverBankBic = (String) execution.getVariable("receiverBankBic");
        String message = (String) execution.getVariable("message");
        Integer amount = (Integer) execution.getVariable("amount");
        Integer commission = (Integer) execution.getVariable("commission");

        try {
            String transactionId = paymentService.createTransaction(
                    senderBillId,
                    receiverBillId,
                    senderBankBic,
                    receiverBankBic,
                    message,
                    amount,
                    commission
            );
            execution.setVariable("transactionId", transactionId);
            execution.setVariable("isPaymentCorrect", true);
        } catch (BillNotFoundException ex) {
            execution.setVariable("isPaymentCorrect", false);
            execution.setVariable("bpmnError", "NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", ex.getMessage());
        } catch (Exception ex) {
            execution.setVariable("isPaymentCorrect", false);
            execution.setVariable("bpmnError", "BAD_REQUEST");
            execution.setVariable("bpmnErrorMessage", "Платеж отклонен");
        }
    }
}