package com.example.sbp.delegate;

import com.example.sbp.exception.BillNotFoundException;
import com.example.sbp.exception.InsufficientFundsException;
import com.example.sbp.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckAmountDelegate implements JavaDelegate {

    private final PaymentService paymentService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("TEST Calculating commission");

        Integer amount = (Integer) execution.getVariable("amount");
        String senderBillId = (String) execution.getVariable("sender_bill_id");
        String senderId = (String) execution.getVariable("senderId");
        String receiverId = (String) execution.getVariable("receiverId");

        try {
            Integer commission = paymentService.checkAmount(amount, senderBillId, senderId, receiverId);
            execution.setVariable("commission", commission);
        } catch (BillNotFoundException notFoundEx) {
            execution.setVariable("isAmountCorrect", false);
            execution.setVariable("bpmnError", "NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", notFoundEx.getMessage());
            return;
        } catch (InsufficientFundsException ex) {
            execution.setVariable("isAmountCorrect", false);
            execution.setVariable("bpmnError", "INSUFFICIENT");
            execution.setVariable("bpmnErrorMessage", "Недостаточно средств на счете отправителя");
            return;
        }
        execution.setVariable("isAmountCorrect", true);
    }
}