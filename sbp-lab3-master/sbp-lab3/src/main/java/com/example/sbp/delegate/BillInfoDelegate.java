package com.example.sbp.delegate;

import com.example.sbp.dto.BillResponseDTO;
import com.example.sbp.exception.TransactionNotFoundException;
import com.example.sbp.service.BillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillInfoDelegate implements JavaDelegate {

    private final BillService billService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String billID = (String) execution.getVariable("billID");
        log.info("TEST 1");
        try {
            BillResponseDTO bill = billService.getBillById(billID);
            execution.setVariable("id", bill.getId());
            execution.setVariable("accountId", bill.getAccountId());
            execution.setVariable("balance", bill.getBalance());
            execution.setVariable("isActive", bill.getIsActive());
            execution.setVariable("isDefault", bill.getIsDefault());
            execution.setVariable("statusFound", true);
        } catch (TransactionNotFoundException e) {
            log.info("TEST 3");
            execution.setVariable("bpmnError", "NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", "Счет не найден: " + billID);
            execution.setVariable("statusFound", false);
        }
    }
}