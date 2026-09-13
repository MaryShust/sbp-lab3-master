package com.example.sbp.delegate;

import com.example.sbp.exception.BankAccountNotFoundException;
import com.example.sbp.service.BillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateBillDelegate implements JavaDelegate {

    private final BillService billService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("TEST");

        String accountId = (String) execution.getVariable("accountID");

        try {
            billService.createBill(accountId);
        } catch (BankAccountNotFoundException e) {
            execution.setVariable("bpmnError", "NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", e.getMessage());
        }

    }
}