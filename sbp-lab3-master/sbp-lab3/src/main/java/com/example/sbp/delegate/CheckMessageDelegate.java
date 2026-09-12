package com.example.sbp.delegate;

import com.example.sbp.exception.MessageFormatException;
import com.example.sbp.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckMessageDelegate implements JavaDelegate {

    private final PaymentService paymentService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String message = (String) execution.getVariable("message");

        log.info("TEST Validating message = " + message);

        try {
            paymentService.checkMessage(message);
        } catch (MessageFormatException e) {
            execution.setVariable("isMessageCorrect", false);
            execution.setVariable("bpmnError", "BAD_REQUEST");
            execution.setVariable("bpmnErrorMessage", e.getMessage());
            return;
        }

        execution.setVariable("isMessageCorrect", true);
    }
}