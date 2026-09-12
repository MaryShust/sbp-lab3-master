package com.example.sbp.entity;

import com.example.sbp.kafka.dto.RiskLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "preSuspicion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreSuspicionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "receiver_account_id", nullable = false, length = 36)
    private String receiverAccountId;

    @Column(name = "receiver_bank_bic", nullable = false, length = 11)
    @Size(min = 8, max = 11, message = "Код BIC банка должен содержать от 8 до 11 символов")
    private String receiverBankBic;

    @Column(name = "sender_account_id", length = 36)
    private String senderAccountId;

    @Column(name = "sender_bill_id", length = 36)
    private String senderBillId;

    @Column(name = "sender_bank_bic", length = 11)
    private String senderBankBic;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "risk_level")
    private RiskLevel riskLevel;
}
