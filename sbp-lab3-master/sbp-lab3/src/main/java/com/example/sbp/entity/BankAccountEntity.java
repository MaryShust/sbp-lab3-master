package com.example.sbp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bank_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountEntity {

    @Id
    private String id;

    @Column(name = "phone_number", unique = true, nullable = false, length = 11)
    @Size(max = 11, message = "Номер телефона не должен превышать 11 символов")
    private String phoneNumber;

    @Column(name = "owner_name", nullable = false, length = 100)
    @Size(max = 100, message = "Имя владельца не должно превышать 100 символов")
    private String ownerName;

    @Column(name = "bank_bic", nullable = false, length = 11)
    @Size(min = 8, max = 11, message = "Код BIC банка должен содержать от 8 до 11 символов")
    private String bankBic;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "default_bill_id", length = 36)
    private String defaultBillId;

    @ElementCollection
    @CollectionTable(
            name = "account_bills",
            joinColumns = @JoinColumn(name = "account_id")
    )
    @Column(name = "bill_id")
    private List<String> allBillIds = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString().replace("-", "0");
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}