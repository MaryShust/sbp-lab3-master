package com.example.sbp.controller;

import com.example.sbp.dto.BillCreateRequestDTO;
import com.example.sbp.dto.BillResponseDTO;
import com.example.sbp.service.BillService;
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
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
@Tag(name = "Bills", description = "Управление счетами (Bill)")
public class BillController {

    private final BillService billService;

    @PostMapping
    @Operation(
            summary = "Создание нового счета",
            description = "Создает новый дополнительный счет (активный сразу). USER - только для своего аккаунта."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Счет успешно создан",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "id": 2,
                                        "accountId": 1,
                                        "isActive": true,
                                        "status": "created"
                                    }
                                    """)))
    })
    public ResponseEntity<?> createBill(@Valid @RequestBody BillCreateRequestDTO billDTO) {
        BillResponseDTO response = billService.createBillWithCheck(billDTO);
        Map<String, Object> result = new HashMap<>();
        result.put("id", response.getId());
        result.put("accountId", response.getAccountId());
        result.put("isActive", response.getIsActive());
        result.put("status", "created");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить счет по ID",
            description = "Возвращает информацию о счете по его ID. USER - только свои счета, MANAGER - все счета."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Счет найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BillResponseDTO.class)))
    })
    public ResponseEntity<?> getBillById(
            @Parameter(description = "ID счета", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id) {
        BillResponseDTO response = billService.getBillByIdWithCheck(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/replenish")
    @Operation(
            summary = "Пополнить счет",
            description = "Пополняет счет на указанную сумму. USER - только свои счета."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Счет успешно пополнен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BillResponseDTO.class)))
    })
    public ResponseEntity<?> replenishBill(
            @Parameter(description = "ID аккаунта", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String accountId,

            @Parameter(description = "ID счета", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id,

            @Parameter(description = "Сумма пополнения", example = "1000")
            @RequestBody Integer amount) {

        BillResponseDTO response = billService.replenishBillWithCheck(accountId, id, amount);
        return ResponseEntity.ok(response);
    }
}