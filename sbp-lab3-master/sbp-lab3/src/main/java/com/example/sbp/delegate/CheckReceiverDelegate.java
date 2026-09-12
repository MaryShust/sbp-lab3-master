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
public class CheckReceiverDelegate implements JavaDelegate {

    private final PaymentService paymentService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("TEST Getting receiver bill");

        String receiverIdentifier = (String) execution.getVariable("receiver_identifier");
        String senderId = (String) execution.getVariable("senderId");

        try {
            Map<String, Object> receiverData = paymentService.checkReceiver(receiverIdentifier, senderId);
            execution.setVariable("receiverId", receiverData.get("receiverId"));
            execution.setVariable("receiverBillId", receiverData.get("receiverBillId"));
            execution.setVariable("receiverBankBic", receiverData.get("receiverBankBic"));
        } catch (BillNotFoundException | BankAccountNotFoundException notFoundEx) {
            execution.setVariable("isReceiverCorrect", false);
            execution.setVariable("bpmnError", "NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", notFoundEx.getMessage());
            return;
        } catch (BillInactiveException billEx) {
            execution.setVariable("isReceiverCorrect", false);
            execution.setVariable("bpmnError", "INACTIVE");
            execution.setVariable("bpmnErrorMessage", billEx.getMessage());
            return;
        }

        execution.setVariable("isReceiverCorrect", true);
    }
}