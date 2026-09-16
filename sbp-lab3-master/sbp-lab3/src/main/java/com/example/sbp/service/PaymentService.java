package com.example.sbp.service;

import com.example.sbp.dto.PaymentRequestDTO;
import com.example.sbp.dto.PaymentResponseDTO;
import com.example.sbp.entity.BankAccountEntity;
import com.example.sbp.entity.BillEntity;
import com.example.sbp.entity.SbpTransactionEntity;
import com.example.sbp.kafka.producer.TransactionEventProducer;
import com.example.sbp.repository.BankAccountRepository;
import com.example.sbp.repository.BillRepository;
import com.example.sbp.repository.SbpTransactionRepository;
import com.example.sbp.exception.*;
import com.example.sbp.security.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final SecurityService securityService;
    private final BillRepository billRepository;
    private final BankAccountRepository accountRepository;
    private final SbpTransactionRepository transactionRepository;
    private final TransactionEventProducer transactionEventProducer;

    private static final double COMMISSION_RATE = 0.005; // 0.5%
    private static final int MIN_COMMISSION = 10;
    private static final int MAX_COMMISSION = 1000;

    @Transactional(transactionManager = "transactionManager")
    public String processPaymentWithCheck(PaymentRequestDTO request) {
        securityService.checkPrivilegeCreatePayment(request.getSenderBillId());

        checkMessage(request.getMessage());
        Map<String, Object> senderData = checkSender(request.getSenderBillId());
        String senderId = (String) senderData.get("senderId");
        String senderBankBic = (String) senderData.get("senderBankBic");

        Map<String, Object> receiverData = checkReceiver(request.getReceiverIdentifier(), senderId);
        String receiverId =  (String) receiverData.get("receiverId");
        String receiverBillId =  (String) receiverData.get("receiverBillId");
        String receiverBankBic =  (String) receiverData.get("receiverBankBic");

        Integer commission = checkAmount(request.getAmount(), request.getSenderBillId(), senderId, receiverId);

        return createTransaction(
                request.getSenderBillId(),
                receiverBillId,
                senderBankBic,
                receiverBankBic,
                request.getMessage(),
                request.getAmount(),
                commission
        );
    }

    @Transactional(transactionManager = "transactionManager")
    public String cancelPaymentWithCheck(String transactionId) {
        SbpTransactionEntity transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Транзакция не найдена по id"));

        revertBalances(
                transaction.getSenderBillId(),
                transaction.getReceiverBillId(),
                transaction.getAmount(),
                transaction.getCommission()
        );

        transactionRepository.delete(transaction);
        log.info("Транзакция {} удалена", transactionId);
        return transactionId;
    }

    private void revertBalances(
            String senderBillId,
            String receiverBillId,
            Integer amount,
            Integer commission
    ) {
        BillEntity senderBillEntity = billRepository.findById(senderBillId)
                .orElseThrow(() -> new BillNotFoundException("Не найден счет отправителя по id: " + senderBillId));

        BillEntity receiverBillEntity = billRepository.findById(receiverBillId)
                .orElseThrow(() -> new BillNotFoundException("Не найден счет получателя по id: " + receiverBillId));

        // Вернуть отправителю
        senderBillEntity.setBalance(senderBillEntity.getBalance() + amount + commission);
        billRepository.save(senderBillEntity);

        // Списать с получателя
        receiverBillEntity.setBalance(receiverBillEntity.getBalance() - amount);
        billRepository.save(receiverBillEntity);
    }

    @Transactional(transactionManager = "transactionManager")
    public PaymentResponseDTO confirmPaymentWithCheck(String transactionId) {
        SbpTransactionEntity transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Транзакция не найдена по id"));

        // Обновить статус транзакции
        transaction.setStatus(SbpTransactionEntity.TransactionStatus.SUCCESS);
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        transactionEventProducer.sendTransactionEvent(transaction);

        return convertToResponseDTO(transaction);
    }

    public void checkMessage(String message) {
        if (message != null && (message.trim().length() > 50 || message.trim().length() < 5)) {
            throw new MessageFormatException("Сообщение не может быть длиннее 50 символов или короче 5 символов, если оно есть");
        }
    }

    public Map<String, Object> checkSender(String senderBillId) {
        BillEntity senderBillEntity = billRepository.findById(senderBillId)
                .orElseThrow(() -> new BillNotFoundException("Не найден счет отправителя по id: " + senderBillId));

        BankAccountEntity senderAccount = accountRepository.findById(senderBillEntity.getAccountId())
                .orElseThrow(() -> new BankAccountNotFoundException("Аккаунт не найден с id: " + senderBillEntity.getAccountId()));

        if (!senderAccount.getIsActive()) {
            throw new BillInactiveException("Аккаунт отправителя не активен");
        }
        if (!senderBillEntity.getIsActive()) {
            throw new BillInactiveException("Счет отправителя не активен");
        }

        HashMap<String, Object> result = new HashMap<>();
        result.put("senderId", senderAccount.getId());
        result.put("senderBankBic", senderAccount.getBankBic());
        return result;
    }

    public Map<String, Object> checkReceiver(String receiverIdentifier, String senderId) {
        BillEntity receiverBillEntity = findReceiverBill(receiverIdentifier);

        // Проверять аккаунт не нужно, так как если он заблочен или на него наложен арест, то деньжата уйдут приставам
        if (!receiverBillEntity.getIsActive()) {
            throw new BillInactiveException("Счет получателя не активен");
        }

        BankAccountEntity receiverAccount = accountRepository.findById(receiverBillEntity.getAccountId())
                .orElseThrow(() -> new BankAccountNotFoundException("Аккаунт не найден с id: " + senderId));

        HashMap<String, Object> result = new HashMap<>();
        result.put("receiverId", receiverAccount.getId());
        result.put("receiverBillId", receiverBillEntity.getId());
        result.put("receiverBankBic", receiverAccount.getBankBic());
        return result;
    }

    public Integer checkAmount(Integer amount, String senderBillId, String senderId, String receiverId) {
        Integer commission = 0;
        if (!senderId.equals(receiverId)) {
            commission = calculateCommission(amount);
        }

        BillEntity senderBillEntity = billRepository.findById(senderBillId)
                .orElseThrow(() -> new BillNotFoundException("Не найден счет отправителя по id: " + senderBillId));

        // Проверить достаточность средств
        int totalAmount = amount + commission;
        if (senderBillEntity.getBalance() < totalAmount) {
            throw new InsufficientFundsException("Недостаточно средств на счете отправителя");
        }
        return commission;
    }

    private BillEntity findReceiverBill(String identifier) {
        // Сначала пробуем найти как ID счета
        Optional<BillEntity> billById = tryFindBillById(identifier);

        // Если нашли по ID - возвращаем
        return billById.orElseGet(() -> findDefaultBillByEmail(identifier));
    }

    private Optional<BillEntity> tryFindBillById(String identifier) {
        try {
            return billRepository.findById(identifier);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private BillEntity findDefaultBillByEmail(String identifier) {
        BankAccountEntity account = accountRepository.findByEmail(identifier)
                .orElseThrow(() -> new BankAccountNotFoundException(
                        "Аккаунт не найден по email: " + identifier));

        return billRepository.findById(account.getDefaultBillId())
                .orElseThrow(() -> new BillNotFoundException(
                        "Дефолтный счет не найден для аккаунта с телефоном: " + identifier));
    }

    private Integer calculateCommission(Integer amount) {
        int commission = (int) Math.round(amount * COMMISSION_RATE);

        // Проверка минимальной и максимальной комиссии
        if (commission < MIN_COMMISSION) {
            return MIN_COMMISSION;
        }
        if (commission > MAX_COMMISSION) {
            return MAX_COMMISSION;
        }
        return commission;
    }

    public String createTransaction(
            String senderBillId,
            String receiverBillId,
            String senderBankBic,
            String receiverBankBic,
            String message,
            Integer amount,
            Integer commission
    ) {
        updateBalances(senderBillId, receiverBillId, amount, commission);

        SbpTransactionEntity transaction = SbpTransactionEntity.builder()
                .senderBillId(senderBillId)
                .senderBankBic(senderBankBic)
                .receiverBillId(receiverBillId)
                .receiverBankBic(receiverBankBic)
                .amount(amount)
                .commission(commission)
                .status(SbpTransactionEntity.TransactionStatus.PENDING)
                .message(message)
                .build();
        return transactionRepository.save(transaction).getTransactionId();
    }

    private void updateBalances(
            String senderBillId,
            String receiverBillId,
            Integer amount,
            Integer commission
    ) {
        BillEntity senderBillEntity = billRepository.findById(senderBillId)
                .orElseThrow(() -> new BillNotFoundException("Не найден счет отправителя по id: " + senderBillId));

        BillEntity receiverBillEntity = billRepository.findById(receiverBillId)
                .orElseThrow(() -> new BillNotFoundException("Не найден счет получателя по id: " + receiverBillId));

        // Списать с отправителя
        senderBillEntity.setBalance(senderBillEntity.getBalance() - amount - commission);
        billRepository.save(senderBillEntity);

        // Зачислить получателю
        receiverBillEntity.setBalance(receiverBillEntity.getBalance() + amount);
        billRepository.save(receiverBillEntity);
    }

    private PaymentResponseDTO convertToResponseDTO(SbpTransactionEntity transaction) {
        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setTransactionId(transaction.getTransactionId());
        response.setStatus(transaction.getStatus().toString());
        response.setSenderBillId(transaction.getSenderBillId());
        response.setReceiverBillId(transaction.getReceiverBillId());
        response.setAmount(transaction.getAmount());
        response.setCommission(transaction.getCommission());
        response.setMessage(transaction.getMessage());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setCompletedAt(transaction.getCompletedAt());

        return response;
    }

    public PaymentResponseDTO getTransactionStatusWithCheck(String transactionId) {
        securityService.checkPrivilegeReadPaymentStatus(transactionId);

        return getTransactionStatus(transactionId);
    }

    public PaymentResponseDTO getTransactionStatus(String transactionId) {
        SbpTransactionEntity transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Транзакция не найдена по id"));

        return convertToResponseDTO(transaction);
    }
}