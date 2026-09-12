package com.example.sbp.delegate;

import com.example.sbp.exception.BankAccountNotFoundException;
import com.example.sbp.exception.BillInactiveException;
import com.example.sbp.exception.BillNotFoundException;
import com.example.sbp.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckSenderDelegate implements JavaDelegate {

    private final PaymentService paymentService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("TEST");

        String senderBillId = (String) execution.getVariable("sender_bill_id");

        try {
            Map<String, Object> senderData = paymentService.checkSender(senderBillId);
            execution.setVariable("senderId", senderData.get("senderId"));
            execution.setVariable("senderBankBic", senderData.get("senderBankBic"));
        } catch (BillNotFoundException | BankAccountNotFoundException notFoundEx) {
            execution.setVariable("isSenderCorrect", false);
            execution.setVariable("bpmnError", "NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", notFoundEx.getMessage());
            return;
        } catch (BillInactiveException billEx) {
            execution.setVariable("isSenderCorrect", false);
            execution.setVariable("bpmnError", "INACTIVE");
            execution.setVariable("bpmnErrorMessage", billEx.getMessage());
            return;
        }

        execution.setVariable("isSenderCorrect", true);
    }
}