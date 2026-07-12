package com.foodmarket.report.service;

import com.foodmarket.report.dto.OrderSummaryResponseDTO;
import com.foodmarket.report.model.OrderSummary;
import com.foodmarket.report.repository.OrderSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final OrderSummaryRepository summaryRepo;

    public OrderSummary recordDeliveredOrder(Long orderId, Long restaurantId, String status, BigDecimal totalAmount) {
        OrderSummary summary = OrderSummary.builder()
                .orderId(orderId)
                .restaurantId(restaurantId)
                .status(status)
                .totalAmount(totalAmount)
                .occurredAt(LocalDateTime.now())
                .build();
        OrderSummary saved = summaryRepo.save(summary);
        log.info("[REPORT] Resumen registrado para pedido {} del restaurante {}", orderId, restaurantId);
        return saved;
    }

    public List<OrderSummaryResponseDTO> getByRestaurant(Long restaurantId) {
        log.info("[REPORT] Consultando reportes para restaurante {}", restaurantId);
        return summaryRepo.findByRestaurantIdOrderByOccurredAtDesc(restaurantId)
                .stream().map(this::toResponse).toList();
    }

    public List<OrderSummaryResponseDTO> getAll() {
        log.info("[REPORT] Consultando reporte global");
        return summaryRepo.findAll()
                .stream().map(this::toResponse).toList();
    }

    private OrderSummaryResponseDTO toResponse(OrderSummary s) {
        return OrderSummaryResponseDTO.builder()
                .id(s.getId())
                .orderId(s.getOrderId())
                .restaurantId(s.getRestaurantId())
                .status(s.getStatus())
                .totalAmount(s.getTotalAmount())
                .occurredAt(s.getOccurredAt())
                .build();
    }
}
