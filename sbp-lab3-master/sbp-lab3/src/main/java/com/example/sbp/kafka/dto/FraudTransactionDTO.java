package com.example.sbp.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudTransactionDTO {
    private String transactionId;
    private String senderBillId;
    private String receiverBillId;
    private String senderBankBic;
    private String receiverBankBic;
    private Integer amount;
    private LocalDateTime createdAt;
}