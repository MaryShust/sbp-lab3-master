package com.example.sbp.delegate;

import com.example.sbp.security.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckPrivilegePaymentDelegate implements JavaDelegate {

    private final SecurityService securityService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("TEST");

        log.info("TEST {}", execution.getVariable("sender_bill_id"));
        String senderBillId = (String) execution.getVariable("sender_bill_id");

        log.info("TEST {}", senderBillId);

        String initiatorId = securityService.getInitiatorId(execution);
        String group = securityService.getInitiatorGroup(initiatorId);

        if (initiatorId != null &&
                "user".equals(group) && securityService.isBillOwnedByCurrentUser(senderBillId, initiatorId)
        ) {
            execution.setVariable("hasPrivilege", true);
            return;
        }

        execution.setVariable("hasPrivilege", false);
        execution.setVariable("bpmnError", "ACCESS_DENIED");
        execution.setVariable("bpmnErrorMessage", "Недостаточно прав для совершения платежа");
    }
}