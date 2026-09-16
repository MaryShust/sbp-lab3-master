package com.example.sbp.delegate;

import com.example.sbp.dto.BankAccountResponseDTO;
import com.example.sbp.exception.BankAccountNotFoundException;
import com.example.sbp.service.BankAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountInfoDelegate implements JavaDelegate {

    private final BankAccountService bankAccountService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String accountID = (String) execution.getVariable("accountID");
        log.info("TEST 1");
        try {
            BankAccountResponseDTO account = bankAccountService.getAccountById(accountID);
            execution.setVariable("id", account.getId());
            execution.setVariable("email", account.getEmail());
            execution.setVariable("ownerName", account.getOwnerName());
            execution.setVariable("bankBic", account.getBankBic());
            execution.setVariable("isActive", account.getIsActive());
            execution.setVariable("defaultBillId", account.getDefaultBillId());
            execution.setVariable("allBillIds", String.join(",", account.getAllBillIds()));
            execution.setVariable("statusFound", true);
        } catch (BankAccountNotFoundException e) {
            log.info("TEST 3");
            execution.setVariable("bpmnError", "NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", "Аккаунт не найден: " + accountID);
            execution.setVariable("statusFound", false);
        }
    }
}