package com.foodmarket.report.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de salida de los reportes de pedidos entregados.
 * Evita exponer la entidad JPA directamente en la API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSummaryResponseDTO {
    private Long id;
    private Long orderId;
    private Long restaurantId;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime occurredAt;
}
