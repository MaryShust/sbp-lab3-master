package com.example.sbp.delegate;

import com.example.sbp.security.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssignInitiatorListener implements ExecutionListener {

    private final SecurityService securityService;

    @Override
    public void notify(DelegateExecution execution) {
        log.info("TEST");
        String initiatorId = securityService.getInitiatorId(execution);
        log.info("TEST {}", initiatorId);
        execution.setVariable("initiator", initiatorId);
    }
}