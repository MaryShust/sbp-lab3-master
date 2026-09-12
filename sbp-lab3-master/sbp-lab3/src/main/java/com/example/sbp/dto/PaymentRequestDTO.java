package com.example.sbp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Запрос на перевод по СБП")
public class PaymentRequestDTO {

    @Schema(
            description = "ID счета отправителя",
            example = "550e8400-e29b-41d4-a716-446655440000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "ID счета отправителя обязателен")
    private String senderBillId;

    @Schema(
            description = "Идентификатор получателя (ID счета или номер телефона)",
            example = "79234567890",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Идентификатор получателя обязателен")
    private String receiverIdentifier; // может быть ID счета или номер телефона

    @Schema(
            description = "Сумма перевода",
            example = "1000",
            minimum = "1",
            maximum = "1000000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Сумма обязательна")
    @Min(value = 1, message = "Минимум 1")
    @Max(value = 1000000, message = "Максимум 1 000 000")
    private Integer amount;

    @Schema(
            description = "Сообщение к переводу",
            example = "Оплата услуг",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String message;
}