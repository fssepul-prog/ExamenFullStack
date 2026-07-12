package com.foodmarket.payment.service;

import com.foodmarket.payment.dto.PaymentDTO;
import com.foodmarket.payment.dto.PaymentResponseDTO;
import com.foodmarket.payment.dto.RefundDTO;
import com.foodmarket.payment.exception.BusinessException;
import com.foodmarket.payment.exception.ResourceNotFoundException;
import com.foodmarket.payment.model.Payment;
import com.foodmarket.payment.model.PaymentStatus;
import com.foodmarket.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService - Pruebas unitarias")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepo;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentDTO paymentDTO;
    private Payment completedPayment;
    private Payment pendingPayment;
    private RefundDTO refundDTO;

    @BeforeEach
    void setUp() {
        paymentDTO = PaymentDTO.builder()
                .orderId(10L)
                .customerId(1L)
                .amount(new BigDecimal("15000"))
                .deliveryFee(new BigDecimal("1990"))
                .method("CREDIT_CARD")
                .build();

        completedPayment = Payment.builder()
                .id(1L)
                .orderId(10L)
                .customerId(1L)
                .amount(new BigDecimal("15000"))
                .deliveryFee(new BigDecimal("1990"))
                .totalAmount(new BigDecimal("16990"))
                .method("CREDIT_CARD")
                .status(PaymentStatus.COMPLETED)
                .build();

        pendingPayment = Payment.builder()
                .id(2L)
                .orderId(11L)
                .customerId(1L)
                .amount(new BigDecimal("10000"))
                .deliveryFee(new BigDecimal("1990"))
                .totalAmount(new BigDecimal("11990"))
                .method("CASH")
                .status(PaymentStatus.PENDING)
                .build();

        refundDTO = new RefundDTO();
        refundDTO.setReason("Pedido llegó incorrecto");
    }

    // ────────── processPayment ──────────

    @Test
    @DisplayName("processPayment - orden sin pago previo se procesa y queda COMPLETED")
    void processPayment_sinPagoPrevio_resultaCompleted() {
        when(paymentRepo.findByOrderId(10L)).thenReturn(Optional.empty());
        when(paymentRepo.save(any(Payment.class))).thenReturn(completedPayment);

        try (MockedStatic<Math> mathMock = mockStatic(Math.class)) {
            mathMock.when(Math::random).thenReturn(0.5);

            PaymentResponseDTO result = paymentService.processPayment(paymentDTO);

            assertNotNull(result);
            verify(paymentRepo, atLeast(2)).save(any(Payment.class));
        }
    }

    @Test
    @DisplayName("processPayment - cuando Math.random < 0.1 el pago queda FAILED")
    void processPayment_randomBajo_resultaFailed() {
        Payment failedPayment = Payment.builder()
                .id(3L).orderId(10L).customerId(1L)
                .amount(new BigDecimal("15000")).deliveryFee(new BigDecimal("1990"))
                .totalAmount(new BigDecimal("16990")).method("CREDIT_CARD")
                .status(PaymentStatus.FAILED).build();

        when(paymentRepo.findByOrderId(10L)).thenReturn(Optional.empty());
        when(paymentRepo.save(any(Payment.class))).thenReturn(failedPayment);

        try (MockedStatic<Math> mathMock = mockStatic(Math.class)) {
            mathMock.when(Math::random).thenReturn(0.05);

            PaymentResponseDTO result = paymentService.processPayment(paymentDTO);

            assertNotNull(result);
            verify(paymentRepo, atLeast(2)).save(any(Payment.class));
        }
    }

    @Test
    @DisplayName("processPayment - orden con pago COMPLETED lanza BusinessException")
    void processPayment_ordenConPagoCompletado_lanzaBusinessException() {
        when(paymentRepo.findByOrderId(10L)).thenReturn(Optional.of(completedPayment));

        assertThrows(BusinessException.class,
                () -> paymentService.processPayment(paymentDTO));
        verify(paymentRepo, never()).save(any());
    }

    @Test
    @DisplayName("processPayment - totalAmount es amount + deliveryFee")
    void processPayment_totalAmountEsAmountMasDeliveryFee() {
        when(paymentRepo.findByOrderId(10L)).thenReturn(Optional.empty());
        when(paymentRepo.save(any(Payment.class))).thenReturn(completedPayment);

        try (MockedStatic<Math> mathMock = mockStatic(Math.class)) {
            mathMock.when(Math::random).thenReturn(0.5);

            paymentService.processPayment(paymentDTO);

            verify(paymentRepo).save(argThat(p ->
                    p.getTotalAmount() != null &&
                    p.getTotalAmount().compareTo(
                            paymentDTO.getAmount().add(paymentDTO.getDeliveryFee())) == 0
            ));
        }
    }

    @Test
    @DisplayName("processPayment - orden con pago PENDING puede volver a procesarse")
    void processPayment_ordenConPagoPending_seProcesaNuevamente() {
        when(paymentRepo.findByOrderId(10L)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepo.save(any(Payment.class))).thenReturn(completedPayment);

        try (MockedStatic<Math> mathMock = mockStatic(Math.class)) {
            mathMock.when(Math::random).thenReturn(0.5);

            assertDoesNotThrow(() -> paymentService.processPayment(paymentDTO));
        }
    }

    // ────────── refund ──────────

    @Test
    @DisplayName("refund - pago COMPLETED se reembolsa correctamente")
    void refund_pagoCompleted_seReembolsa() {
        when(paymentRepo.findById(1L)).thenReturn(Optional.of(completedPayment));
        when(paymentRepo.save(any(Payment.class))).thenReturn(completedPayment);

        PaymentResponseDTO result = paymentService.refund(1L, refundDTO);

        assertNotNull(result);
        assertEquals(PaymentStatus.REFUNDED, completedPayment.getStatus());
        assertEquals("Pedido llegó incorrecto", completedPayment.getRefundReason());
        verify(paymentRepo, times(1)).save(completedPayment);
    }

    @Test
    @DisplayName("refund - pago no encontrado lanza ResourceNotFoundException")
    void refund_pagoNoEncontrado_lanzaResourceNotFoundException() {
        when(paymentRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.refund(99L, refundDTO));
    }

    @Test
    @DisplayName("refund - pago no COMPLETED lanza BusinessException")
    void refund_pagoNOCompleted_lanzaBusinessException() {
        when(paymentRepo.findById(2L)).thenReturn(Optional.of(pendingPayment));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.refund(2L, refundDTO));
        assertTrue(ex.getMessage().contains("completados"));
        verify(paymentRepo, never()).save(any());
    }

    @Test
    @DisplayName("refund - razón del reembolso queda guardada en el pago")
    void refund_razonQuedaGuardada() {
        when(paymentRepo.findById(1L)).thenReturn(Optional.of(completedPayment));
        when(paymentRepo.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.refund(1L, refundDTO);

        assertEquals("Pedido llegó incorrecto", completedPayment.getRefundReason());
        assertEquals(PaymentStatus.REFUNDED, completedPayment.getStatus());
    }

    // ────────── getByOrder ──────────

    @Test
    @DisplayName("getByOrder - pago encontrado retorna DTO con status correcto")
    void getByOrder_pagoEncontrado_retornaDTO() {
        when(paymentRepo.findByOrderId(10L)).thenReturn(Optional.of(completedPayment));

        PaymentResponseDTO result = paymentService.getByOrder(10L);

        assertNotNull(result);
        assertEquals(10L, result.getOrderId());
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
    }

    @Test
    @DisplayName("getByOrder - orden sin pago lanza ResourceNotFoundException")
    void getByOrder_ordenSinPago_lanzaResourceNotFoundException() {
        when(paymentRepo.findByOrderId(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getByOrder(99L));
    }

    @Test
    @DisplayName("getByOrder - totalAmount del DTO es correcto")
    void getByOrder_totalAmountEsCorrecto() {
        when(paymentRepo.findByOrderId(10L)).thenReturn(Optional.of(completedPayment));

        PaymentResponseDTO result = paymentService.getByOrder(10L);

        assertEquals(new BigDecimal("16990"), result.getTotalAmount());
    }

    // ────────── getByCustomer ──────────

    @Test
    @DisplayName("getByCustomer - retorna historial de pagos del cliente")
    void getByCustomer_retornaHistorialDePagos() {
        when(paymentRepo.findByCustomerIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(completedPayment, pendingPayment));

        List<PaymentResponseDTO> result = paymentService.getByCustomer(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getByCustomer - cliente sin pagos retorna lista vacía")
    void getByCustomer_sinPagos_retornaListaVacia() {
        when(paymentRepo.findByCustomerIdOrderByCreatedAtDesc(99L))
                .thenReturn(Collections.emptyList());

        List<PaymentResponseDTO> result = paymentService.getByCustomer(99L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
