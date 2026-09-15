package com.example.sbp.delegate;

import com.example.sbp.exception.BankAccountNotFoundException;
import com.example.sbp.exception.BillNotFoundException;
import com.example.sbp.security.SecurityService;
import com.example.sbp.service.BankAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivateDefaultBillDelegate implements JavaDelegate {

    private final SecurityService securityService;
    private final BankAccountService bankAccountService;

    @Override
    public void execute(DelegateExecution execution) {
        String accountID = (String) execution.getVariable("accountID");
        Integer startBalance = (Integer) execution.getVariable("startBalance");

        try {
            bankAccountService.activateDefaultBill(accountID, startBalance);
            execution.setVariable("isActivateCorrect", true);
        } catch (BillNotFoundException | BankAccountNotFoundException notFoundEx) {
            execution.setVariable("isActivateCorrect", false);
            execution.setVariable("bpmnError", "NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", notFoundEx.getMessage());
        }
    }
}