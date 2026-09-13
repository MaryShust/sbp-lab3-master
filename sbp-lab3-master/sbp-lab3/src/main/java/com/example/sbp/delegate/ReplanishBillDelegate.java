package com.example.sbp.delegate;

import com.example.sbp.exception.BankAccountNotFoundException;
import com.example.sbp.exception.BillInactiveException;
import com.example.sbp.exception.BillNotBelongAccountExeption;
import com.example.sbp.exception.BillNotFoundException;
import com.example.sbp.service.BillService;
import lombok.RequiredArgsConstructor;
        import lombok.extern.slf4j.Slf4j;
        import org.camunda.bpm.engine.delegate.DelegateExecution;
        import org.camunda.bpm.engine.delegate.JavaDelegate;
        import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReplanishBillDelegate implements JavaDelegate {

    private final BillService billService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("TEST");

        String accountId = (String) execution.getVariable("accountID");
        String billId = (String) execution.getVariable("billID");
        Integer amount = (Integer) execution.getVariable("amount");

        try {
            billService.replenishBill(accountId, billId, amount);
        } catch (BankAccountNotFoundException | BillNotFoundException e) {
            execution.setVariable("bpmnError", "NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", e.getMessage());
        } catch (BillNotBelongAccountExeption e) {
            execution.setVariable("bpmnError", "INTERNAL_SERVER_ERROR");
            execution.setVariable("bpmnErrorMessage", e.getMessage());
        } catch (BillInactiveException e) {
            execution.setVariable("bpmnError", "CONFLICT");
            execution.setVariable("bpmnErrorMessage", e.getMessage());
        }

    }
}