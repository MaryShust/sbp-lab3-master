package com.example.sbp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на регистрацию")
public class RegisterRequestDTO {

    @NotBlank(message = "Имя пользователя обязательно")
    @Schema(
            description = "Имя пользователя",
            example = "user",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;

    @NotBlank(message = "Пароль обязателен")
    @Schema(
            description = "Пароль",
            example = "password123",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;

    @NotBlank(message = "Email обязателен")
    @Schema(
            description = "Email пользователя",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @NotBlank(message = "Имя обязательно")
    @Schema(
            description = "Имя",
            example = "Иван",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    @Schema(
            description = "Фамилия",
            example = "Петров",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String lastName;
}
