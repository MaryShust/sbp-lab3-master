package com.example.sbp.security;

import com.example.sbp.exception.AccessDeniedException;
import com.example.sbp.repository.BillRepository;
import com.example.sbp.repository.SbpTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityService {

    private final BillRepository billRepository;
    private final SbpTransactionRepository transactionRepository;
    private final IdentityService identityService;

    public CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        return null;
    }

    public void checkPrivilegeReadAccount(String accountId) {
        CustomUserDetails user = getCurrentUser();
        log.info("SecurityService checkPrivilegeReadAccount: {}", user);

        if (user != null && user.hasPrivilege(Privilege.ACCOUNT_SUPER_READ)) {
            return;
        }

        if (user != null &&
                user.hasPrivilege(Privilege.ACCOUNT_READ) &&
                user.hasPrivilege(Privilege.ACCOUNT_READ_BY_PHONE) &&
                user.getAccountId() != null &&
                user.getAccountId().equals(accountId)
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для получения информации об аккаунте");
    }

    public void checkPrivilegeActivateAccount(String accountId) {
        CustomUserDetails user = getCurrentUser();

        if (user != null &&
                user.hasPrivilege(Privilege.ACCOUNT_ACTIVATE) &&
                user.getAccountId() != null && user.getAccountId().equals(accountId)
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для активации аккаунта");
    }

    public void checkPrivilegeCreateBill(String accountId) {
        CustomUserDetails user = getCurrentUser();

        if (user != null &&
                user.hasPrivilege(Privilege.BILL_CREATE) &&
                user.getAccountId() != null && user.getAccountId().equals(accountId)
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для создания счета");
    }

    public void checkPrivilegeReadBill(String billId) {
        CustomUserDetails user = getCurrentUser();

        if (user != null && user.hasPrivilege(Privilege.BILL_SUPER_READ)) {
            return;
        }

        if (user != null &&
                user.hasPrivilege(Privilege.BILL_READ) &&
                user.hasPrivilege(Privilege.BILL_READ_DEFAULT) &&
                isBillOwnedByCurrentUser(billId, user.getAccountId())
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для получения информации о счета");
    }

    public void checkPrivilegeReplenishBill(String billId) {
        CustomUserDetails user = getCurrentUser();

        if (user != null &&
                user.hasPrivilege(Privilege.BILL_REPLENISH) &&
                isBillOwnedByCurrentUser(billId, user.getAccountId())
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для пополнения счета");
    }

    public void checkPrivilegeReadPaymentStatus(String transactionId) {
        CustomUserDetails user = getCurrentUser();

        if (user != null && user.hasPrivilege(Privilege.PAYMENT_SUPER_READ_STATUS)) {
            return;
        }

        if (user != null &&
                user.hasPrivilege(Privilege.PAYMENT_READ_STATUS) &&
                isTransactionRelatedToCurrentUser(transactionId, user.getAccountId())
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для чтения статусов транзакции");
    }

    public void checkPrivilegeCreatePayment(String senderBillId) {
        CustomUserDetails user = getCurrentUser();

        if (user != null &&
                user.hasPrivilege(Privilege.PAYMENT_CREATE) && isBillOwnedByCurrentUser(senderBillId, user.getAccountId())
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для создания перевода");
    }

    public boolean isBillOwnedByCurrentUser(String billId, String accountId) {
        if (accountId == null) return false;

        return billRepository.findById(billId)
                .map(bill -> bill.getAccountId().equals(accountId))
                .orElse(false);
    }

    public boolean isTransactionRelatedToCurrentUser(String transactionId, String accountId) {
        if (accountId == null) return false;

        return transactionRepository.findByTransactionId(transactionId)
                .map(tx -> {
                    boolean isSender = isBillOwnedByCurrentUser(tx.getSenderBillId(), accountId);
                    boolean isReceiver = isBillOwnedByCurrentUser(tx.getReceiverBillId(), accountId);
                    return isSender || isReceiver;
                })
                .orElse(false);
    }


    public String getInitiatorId(DelegateExecution execution) {
        String initiator = (String) execution.getVariable("initiator");
        if (initiator != null && !initiator.isBlank()) {
            return initiator;
        }
        return identityService.getCurrentAuthentication().getUserId();
    }

    public String getInitiatorName(DelegateExecution execution, String initiatorId) {
        String userName = (String) execution.getVariable("userName");
        if (userName != null && !userName.isBlank()) {
            return userName;
        }
        if (initiatorId == null) {
            return null;
        }
        User user = identityService.createUserQuery().userId(initiatorId).singleResult();
        if (user != null) {
            return String.join(" ", nonNull(user.getFirstName()), nonNull(user.getLastName())).trim();
        }
        return initiatorId;
    }

    public String getInitiatorGroup(String initiatorId) {
        if (initiatorId == null) {
            return null;
        }
        List<Group> groups = identityService.createGroupQuery().groupMember(initiatorId).list();
        if (groups == null || groups.isEmpty()) {
            return null;
        }
        return groups.get(0).getName() != null ? groups.get(0).getName() : groups.get(0).getId();
    }

    private static String nonNull(String value) {
        return value != null ? value : "";
    }
}
