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
public class CheckPrivilegeAccountInfoDelegate implements JavaDelegate {

    private final SecurityService securityService;

    @Override
    public void execute(DelegateExecution execution) {
        String transactionId = (String) execution.getVariable("transactionId");

        String initiatorId = securityService.getInitiatorId(execution);
        String group = securityService.getInitiatorGroup(initiatorId);

        log.info("TEST 1 initiatorId = {}", initiatorId);
        log.info("TEST 1 group = {}", group);

        execution.setVariable("initiatorId", initiatorId);
        execution.setVariable("userGroup", group);

        if ("manager".equals(group)) {
            execution.setVariable("hasPrivilege", true);
            log.info("TEST 2");
            return;
        }

        if ("user".equals(group) &&
                securityService.isTransactionRelatedToCurrentUser(transactionId, initiatorId)
        ) {
            execution.setVariable("hasPrivilege", true);
            log.info("TEST 3");
            return;
        }

        log.info("TEST 4");
        execution.setVariable("bpmnError", "ACCESS_DENIED");
        execution.setVariable("bpmnErrorMessage", "Недостаточно прав для просмотра статуса транзакции");
        execution.setVariable("hasPrivilege", false);
    }
}