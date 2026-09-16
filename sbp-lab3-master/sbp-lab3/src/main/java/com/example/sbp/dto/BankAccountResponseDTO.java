package com.example.sbp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Ответ с данными аккаунта")
public class BankAccountResponseDTO {

    @Schema(description = "ID аккаунта", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "Email", example = "test@mail.ru")
    private String email;

    @Schema(description = "Имя владельца", example = "Иван Петров")
    private String ownerName;

    @Schema(description = "БИК банка", example = "044525555")
    private String bankBic;

    @Schema(description = "Активен ли аккаунт", example = "true")
    private Boolean isActive;

    @Schema(description = "Дата создания", example = "2024-01-01T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Дата обновления", example = "2024-01-01T12:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "ID дефолтного счета", example = "550e8400-e29b-41d4-a716-446655440000")
    private String defaultBillId;

    @Schema(description = "Список ID всех счетов", example = "[\"550e8400-e29b-41d4-a716-446655440000\"]")
    private List<String> allBillIds;
}