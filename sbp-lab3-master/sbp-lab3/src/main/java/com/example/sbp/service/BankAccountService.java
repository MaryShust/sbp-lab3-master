package com.example.sbp.service;

import com.example.sbp.dto.BankAccountResponseDTO;
import com.example.sbp.entity.BankAccountEntity;
import com.example.sbp.entity.BillEntity;
import com.example.sbp.exception.*;
import com.example.sbp.repository.BankAccountRepository;
import com.example.sbp.repository.BillRepository;
import com.example.sbp.security.SecurityService;
import com.example.sbp.security.XmlUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankAccountService {

    private final BankAccountRepository accountRepository;
    private final BillRepository billRepository;
    private final XmlUserDetailsService userDetailsService;
    private final SecurityService securityService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BankAccountResponseDTO createAccount(
            String ownerName,
            String email,
            String bankBic
    ) {
        log.debug("Создать новый аккаунт с email: {}", email);

        validateOwnerName(ownerName);
        validateBankBic(bankBic);
        validateEmail(email);

        // Проверка уникальности на телефон
        if (accountRepository.existsByEmail(email)) {
            throw new BankAccountAlreadyExistsException("Email уже существует");
        }

        BankAccountEntity account = BankAccountEntity.builder()
                .email(email)
                .ownerName(ownerName)
                .bankBic(bankBic)
                .isActive(true)
                .allBillIds(new ArrayList<>())
                .build();

        account = accountRepository.save(account);
        log.debug("Аккаунт сохранен с ID: {}", account.getId());


        BillEntity defaultBillEntity = BillEntity.builder()
                .accountId(account.getId())
                .balance(0)
                .isActive(false)  // дефолтный счет требуется в дальнейшем активировать
                .build();

        defaultBillEntity = billRepository.save(defaultBillEntity);
        log.debug("Дефолтный счет создан с ID: {} (inactive)", defaultBillEntity.getId());

        // Обновление всего и вся
        account.setDefaultBillId(defaultBillEntity.getId());
        account.getAllBillIds().add(defaultBillEntity.getId());
        account = accountRepository.save(account);

        userDetailsService.updateUserAccountIdByEmail(account.getEmail(), account.getId());
        log.debug("Связали аккаунт {} с пользователем с email {}", account.getId(), account.getEmail());

        return mapToResponseDTO(account);
    }

    public BankAccountResponseDTO getAccountByIdWithCheck(String id) {
        securityService.checkPrivilegeReadAccount(id);
        return  getAccountById(id);
    }

    public BankAccountResponseDTO getAccountById(String id) {
        BankAccountEntity account = accountRepository.findById(id)
                .orElseThrow(() -> new BankAccountNotFoundException("Аккаунт не найден с id: " + id));
        return mapToResponseDTO(account);
    }

    public void activateDefaultBillWithCheck(String accountId, Integer startBalance) {
        securityService.checkPrivilegeActivateAccount(accountId);
        activateDefaultBill(accountId, startBalance);
    }

    @Transactional
    public void activateDefaultBill(String accountId, Integer startBalance) {
        BankAccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BankAccountNotFoundException("Аккаунт не найден с id: " + accountId));

        BillEntity defaultBillEntity = billRepository.findById(account.getDefaultBillId())
                .orElseThrow(() -> new BillNotFoundException("Дефолтный счет не найден"));

        // Баланс должен быть положительным
        if (defaultBillEntity.getBalance() == 0) {
            defaultBillEntity.setIsActive(true);
            defaultBillEntity.setBalance(startBalance);
            billRepository.save(defaultBillEntity);
            log.debug("Дефолтный счет {} активирован для аккаунта {}", defaultBillEntity.getId(), accountId);
        }
    }

    private BankAccountResponseDTO mapToResponseDTO(BankAccountEntity account) {
        BankAccountResponseDTO dto = new BankAccountResponseDTO();
        dto.setId(account.getId());
        dto.setEmail(account.getEmail());
        dto.setOwnerName(account.getOwnerName());
        dto.setBankBic(account.getBankBic());
        dto.setIsActive(account.getIsActive());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());
        dto.setDefaultBillId(account.getDefaultBillId());
        dto.setAllBillIds(account.getAllBillIds());
        return dto;
    }

    private void validateOwnerName(String ownerName) {
        if (ownerName == null || ownerName.isBlank()) {
            throw new OwnerNameFormatException("Имя не может быть пустым");
        }

        String trimmed = ownerName.trim();
        if (trimmed.length() > 100) {
            throw new OwnerNameFormatException(
                    String.format("Имя владельца не должно превышать 100 символов, текущая длина: %d",
                            trimmed.length())
            );
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new EmailFormatException("Email не может быть пустым");
        }

        String trimmed = email.trim();
        if (!trimmed.matches("^.+@mail.ru$")) {
            throw new EmailFormatException("Неверный формат email");
        }
    }

    private void validateBankBic(String bankBic) {
        if (bankBic == null || bankBic.isBlank()) {
            throw new BankBicFormatException("BIC не может быть пустым");
        }

        String trimmed = bankBic.trim();

        // Проверка длины (BIC должен быть 8 или 11 символов)
        int length = trimmed.length();
        if (length < 8 || length > 11) {
            throw new BankBicFormatException(
                    String.format("Код BIC банка должен состоять из 8 или 11 символов, текущая длина: %d", length)
            );
        }

        // Проверка на запрещенные символы в BIC
        if (trimmed.contains(" ")) {
            throw new BankBicFormatException("BIC не может содержать пробелы");
        }
    }
}