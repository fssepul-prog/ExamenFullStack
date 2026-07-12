package com.foodmarket.report.service;

import com.foodmarket.report.dto.OrderSummaryResponseDTO;
import com.foodmarket.report.model.OrderSummary;
import com.foodmarket.report.repository.OrderSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService - Pruebas unitarias")
class ReportServiceTest {

    @Mock
    private OrderSummaryRepository summaryRepo;

    @InjectMocks
    private ReportService reportService;

    private OrderSummary summary;

    @BeforeEach
    void setUp() {
        summary = OrderSummary.builder()
                .id(1L)
                .orderId(100L)
                .restaurantId(5L)
                .status("DELIVERED")
                .totalAmount(new BigDecimal("18990"))
                .occurredAt(LocalDateTime.now())
                .build();
    }

    // ────────── recordDeliveredOrder ──────────

    @Test
    @DisplayName("recordDeliveredOrder - guarda el resumen con los datos del pedido")
    void recordDeliveredOrder_guardaResumenCorrectamente() {
        when(summaryRepo.save(any(OrderSummary.class))).thenReturn(summary);

        OrderSummary result = reportService.recordDeliveredOrder(
                100L, 5L, "DELIVERED", new BigDecimal("18990"));

        assertNotNull(result);
        assertEquals(100L, result.getOrderId());
        assertEquals(5L, result.getRestaurantId());
        assertEquals("DELIVERED", result.getStatus());
        verify(summaryRepo, times(1)).save(any(OrderSummary.class));
    }

    @Test
    @DisplayName("recordDeliveredOrder - totalAmount se guarda correctamente")
    void recordDeliveredOrder_totalAmountCorrecto() {
        when(summaryRepo.save(any(OrderSummary.class))).thenReturn(summary);

        OrderSummary result = reportService.recordDeliveredOrder(
                100L, 5L, "DELIVERED", new BigDecimal("18990"));

        assertEquals(new BigDecimal("18990"), result.getTotalAmount());
    }

    @Test
    @DisplayName("recordDeliveredOrder - occurredAt queda establecido")
    void recordDeliveredOrder_occurredAtEstablecido() {
        when(summaryRepo.save(any(OrderSummary.class))).thenReturn(summary);

        OrderSummary result = reportService.recordDeliveredOrder(
                100L, 5L, "DELIVERED", new BigDecimal("18990"));

        assertNotNull(result.getOccurredAt());
    }

    @Test
    @DisplayName("recordDeliveredOrder - el resumen persistido contiene el restaurantId correcto")
    void recordDeliveredOrder_restaurantIdCorrecto() {
        when(summaryRepo.save(any(OrderSummary.class))).thenReturn(summary);

        reportService.recordDeliveredOrder(100L, 5L, "DELIVERED", new BigDecimal("18990"));

        verify(summaryRepo).save(argThat(s ->
                s.getOrderId().equals(100L) &&
                s.getRestaurantId().equals(5L) &&
                s.getOccurredAt() != null
        ));
    }

    // ────────── getByRestaurant ──────────

    @Test
    @DisplayName("getByRestaurant - retorna reportes del restaurante ordenados por fecha")
    void getByRestaurant_retornaReportes() {
        OrderSummary summary2 = OrderSummary.builder()
                .id(2L).orderId(101L).restaurantId(5L).status("DELIVERED")
                .totalAmount(new BigDecimal("9990")).occurredAt(LocalDateTime.now()).build();
        when(summaryRepo.findByRestaurantIdOrderByOccurredAtDesc(5L))
                .thenReturn(List.of(summary, summary2));

        List<OrderSummaryResponseDTO> result = reportService.getByRestaurant(5L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(5L, result.get(0).getRestaurantId());
    }

    @Test
    @DisplayName("getByRestaurant - restaurante sin pedidos retorna lista vacía")
    void getByRestaurant_sinPedidos_retornaListaVacia() {
        when(summaryRepo.findByRestaurantIdOrderByOccurredAtDesc(99L))
                .thenReturn(Collections.emptyList());

        List<OrderSummaryResponseDTO> result = reportService.getByRestaurant(99L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ────────── getAll ──────────

    @Test
    @DisplayName("getAll - retorna todos los reportes del sistema")
    void getAll_retornaTodosLosReportes() {
        OrderSummary summary2 = OrderSummary.builder()
                .id(2L).orderId(201L).restaurantId(7L).status("DELIVERED")
                .totalAmount(new BigDecimal("25000")).occurredAt(LocalDateTime.now()).build();
        when(summaryRepo.findAll()).thenReturn(List.of(summary, summary2));

        List<OrderSummaryResponseDTO> result = reportService.getAll();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getAll - sin reportes retorna lista vacía")
    void getAll_sinReportes_retornaListaVacia() {
        when(summaryRepo.findAll()).thenReturn(Collections.emptyList());

        List<OrderSummaryResponseDTO> result = reportService.getAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getAll - invoca findAll exactamente una vez")
    void getAll_invocaFindAllUnaVez() {
        when(summaryRepo.findAll()).thenReturn(List.of(summary));

        reportService.getAll();

        verify(summaryRepo, times(1)).findAll();
    }
}
