package com.example.sbp.controller;

import com.example.sbp.dto.BankAccountRequestDTO;
import com.example.sbp.dto.BankAccountResponseDTO;
import com.example.sbp.service.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Управление аккаунтами пользователей")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @PostMapping
    @PreAuthorize("hasAuthority('ACCOUNT_CREATE')")
    @Operation(
            summary = "Создание нового аккаунта",
            description = "Создает новый аккаунт и дефолтный счет (неактивный). Только для MANAGER в офисе банка."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аккаунт успешно создан",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "id": 1,
                                        "email": "test@mail.ru",
                                        "defaultBillId": 1,
                                        "status": "created",
                                        "message": "Account created. Default bill is inactive - please fund it to activate"
                                    }
                                    """)))
    })
    public ResponseEntity<?> createAccount(@Valid @RequestBody BankAccountRequestDTO bankAccountRequestDTO) {
        BankAccountResponseDTO response = bankAccountService.createAccount(
                bankAccountRequestDTO.getOwnerName(),
                bankAccountRequestDTO.getEmail(),
                bankAccountRequestDTO.getBankBic()
        );
        Map<String, Object> result = new HashMap<>();
        result.put("id", response.getId());
        result.put("email", response.getEmail());
        result.put("defaultBillId", response.getDefaultBillId());
        result.put("status", "created");
        result.put("message", "Счет создан. Пополните для активации");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить аккаунт по ID",
            description = "Возвращает информацию об аккаунте по его ID. USER - только свой аккаунт, MANAGER - любой."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аккаунт найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BankAccountResponseDTO.class))),
    })
    public ResponseEntity<?> getAccountById(
            @Parameter(description = "ID аккаунта", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id
    ) {
        BankAccountResponseDTO response = bankAccountService.getAccountByIdWithCheck(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/activate-default")
    @Operation(
            summary = "Активировать дефолтный счет",
            description = "Пополняет и активирует дефолтный счет аккаунта. USER - только свой аккаунт."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Счет успешно активирован",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "message": "Default bill activated successfully"
                                    }
                                    """)))
    })
    public ResponseEntity<?> activateDefaultBill(
            @Parameter(description = "ID аккаунта", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String accountId,
            @Parameter(description = "Начальный баланс для активации", example = "1000")
            @RequestParam(required = true) Integer startBalance
    ) {
        bankAccountService.activateDefaultBillWithCheck(accountId, startBalance);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Дефолтный счет активирован");
        return ResponseEntity.ok(response);
    }
}