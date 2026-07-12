package com.foodmarket.payment.controller;

import com.foodmarket.payment.dto.PaymentDTO;
import com.foodmarket.payment.dto.PaymentResponseDTO;
import com.foodmarket.payment.dto.RefundDTO;
import com.foodmarket.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pagos", description = "Procesamiento de pagos y reembolsos")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Procesar pago",
            description = "Registra el pago de un pedido. Métodos aceptados: CREDIT_CARD, DEBIT_CARD, CASH, TRANSFER")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pago procesado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de pago inválidos"),
        @ApiResponse(responseCode = "409", description = "El pedido ya tiene un pago registrado")
    })
    @PostMapping
    public ResponseEntity<PaymentResponseDTO> process(@Valid @RequestBody PaymentDTO dto) {
        log.info("Procesando pago para orden {}", dto.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.processPayment(dto));
    }

    @Operation(summary = "Obtener pago por ID de pedido")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pago encontrado"),
        @ApiResponse(responseCode = "404", description = "Pago no encontrado para el pedido")
    })
    @GetMapping("/order/{id}")
    public ResponseEntity<PaymentResponseDTO> getByOrder(
            @Parameter(description = "ID del pedido") @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getByOrder(id));
    }

    @Operation(summary = "Obtener historial de pagos de un cliente")
    @ApiResponse(responseCode = "200", description = "Lista de pagos del cliente")
    @GetMapping("/customer/{id}")
    public ResponseEntity<List<PaymentResponseDTO>> getByCustomer(
            @Parameter(description = "ID del cliente") @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getByCustomer(id));
    }

    @Operation(summary = "Emitir reembolso (solo ADMIN)",
            description = "Revierte un pago y registra el motivo del reembolso. Solo accesible con rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reembolso procesado exitosamente"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN"),
        @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponseDTO> refund(
            @Parameter(description = "ID del pago") @PathVariable Long id,
            @Valid @RequestBody RefundDTO dto,
            @RequestHeader(value = "X-User-Role", defaultValue = "ADMIN") String role) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        log.info("Reembolso solicitado por ADMIN para pago {}", id);
        return ResponseEntity.ok(paymentService.refund(id, dto));
    }
}
