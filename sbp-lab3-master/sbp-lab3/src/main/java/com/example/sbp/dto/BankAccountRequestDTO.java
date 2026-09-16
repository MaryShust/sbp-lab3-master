package com.example.sbp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Запрос на создание аккаунта")
public class BankAccountRequestDTO {

    @Schema(
            description = "Email пользователя (для привязки accountId в users.xml)",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email обязателен")
    private String email;

    @Schema(
            description = "Имя владельца",
            example = "Иван Петров",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Имя обязательно")
    private String ownerName;

    @Schema(
            description = "БИК банка",
            example = "044525555",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "БИК обязателен")
    @Pattern(regexp = "^[0-9]{9}$", message = "БИК: 9 цифр")
    private String bankBic;
}