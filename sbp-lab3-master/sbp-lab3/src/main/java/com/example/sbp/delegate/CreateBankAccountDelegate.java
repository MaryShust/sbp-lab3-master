package com.example.sbp.delegate;

import com.example.sbp.dto.BankAccountResponseDTO;
import com.example.sbp.exception.*;
import com.example.sbp.service.BankAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateBankAccountDelegate implements JavaDelegate {

    private final BankAccountService bankAccountService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("TEST");

        String ownerName = (String) execution.getVariable("ownerName");
        String email = (String) execution.getVariable("email");
        String bankBic = (String) execution.getVariable("bankBic");

        try {
            BankAccountResponseDTO responseDTO = bankAccountService.createAccount(ownerName, email, bankBic);
            execution.setVariable("id", responseDTO.getId());
            execution.setVariable("email", responseDTO.getEmail());
            execution.setVariable("ownerName", responseDTO.getOwnerName());
            execution.setVariable("bankBic", responseDTO.getBankBic());
            execution.setVariable("isActive", responseDTO.getIsActive());
            execution.setVariable("defaultBillId", responseDTO.getDefaultBillId());
        } catch (OwnerNameFormatException | EmailFormatException | BankBicFormatException ex1) {
            execution.setVariable("bpmnError", "BAD_REQUEST");
            execution.setVariable("bpmnErrorMessage", ex1.getMessage());
        } catch (BankAccountAlreadyExistsException | FileParseException ex2) {
            execution.setVariable("bpmnError", "CONFLICT");
            execution.setVariable("bpmnErrorMessage", ex2.getMessage());
        }
    }
}