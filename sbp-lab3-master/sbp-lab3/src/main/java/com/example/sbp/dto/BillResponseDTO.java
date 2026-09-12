package com.example.sbp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "Ответ с данными счета")
public class BillResponseDTO {

    @Schema(description = "ID счета", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "ID аккаунта владельца", example = "550e8400-e29b-41d4-a716-446655440000")
    private String accountId;

    @Schema(description = "Баланс", example = "1500")
    private Integer balance;

    @Schema(description = "Активен ли счет", example = "true")
    private Boolean isActive;

    @Schema(description = "Является ли дефолтным", example = "false")
    private Boolean isDefault;

    @Schema(description = "Дата создания", example = "2024-01-01T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Дата обновления", example = "2024-01-01T12:00:00")
    private LocalDateTime updatedAt;
}