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
public class CheckPrivilegeCreateBillDelegate implements JavaDelegate {

    private final SecurityService securityService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("TEST");

        String accountId = (String) execution.getVariable("accountID");
        String initiatorId = securityService.getInitiatorId(execution);
        String group = securityService.getInitiatorGroup(initiatorId);

        if (accountId != null &&
                group.equals("user") &&
                accountId.equals(initiatorId)
        ) {
            execution.setVariable("hasPrivilege", true);
            return;
        }

        execution.setVariable("hasPrivilege", false);
        execution.setVariable("bpmnError", "ACCESS_DENIED");
        execution.setVariable("bpmnErrorMessage", "Недостаточно прав для создания счета");

    }
}