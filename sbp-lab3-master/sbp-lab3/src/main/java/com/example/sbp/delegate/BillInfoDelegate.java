package com.example.sbp.delegate;

import com.example.sbp.dto.BankAccountResponseDTO;
import com.example.sbp.dto.PaymentResponseDTO;
import com.example.sbp.exception.TransactionNotFoundException;
import com.example.sbp.service.BankAccountService;
import com.example.sbp.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillInfoDelegate implements JavaDelegate {

    private final BankAccountService bankAccountService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String accountID = (String) execution.getVariable("accountID");
        log.info("TEST 1");
        try {
            BankAccountResponseDTO account = bankAccountService.getAccountById(accountID);
            execution.setVariable("id", account.getId());
            execution.setVariable("phoneNumber", account.getPhoneNumber());
            execution.setVariable("ownerName", account.getOwnerName());
            execution.setVariable("bankBic", account.getBankBic());
            execution.setVariable("isActive", account.getIsActive());
            execution.setVariable("createdAt", account.getCreatedAt());
            execution.setVariable("updatedAt", account.getUpdatedAt());
            execution.setVariable("defaultBillId", account.getDefaultBillId());
            ArrayList<String> test = new ArrayList();

            List<String> test2 = account.getAllBillIds();
            test.addAll(test2);
            log.info("TEST 2");
            execution.setVariable("statusFound", true);
        } catch (TransactionNotFoundException e) {
            log.info("TEST 3");
            execution.setVariable("bpmnError", "NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", "Аккаунт найден: " + accountID);
            execution.setVariable("statusFound", false);
        }
    }
}