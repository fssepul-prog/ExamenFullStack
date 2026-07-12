package com.foodmarket.review.service;

import com.foodmarket.review.client.OrderFeignClient;
import com.foodmarket.review.dto.ReviewDTO;
import com.foodmarket.review.dto.ReviewResponseDTO;
import com.foodmarket.review.exception.BusinessException;
import com.foodmarket.review.model.Review;
import com.foodmarket.review.model.TargetType;
import com.foodmarket.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService - Pruebas unitarias")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepo;

    @Mock
    private OrderFeignClient orderClient;

    @InjectMocks
    private ReviewService reviewService;

    private ReviewDTO reviewDTO;
    private Review savedReview;
    private OrderFeignClient.OrderStatusDTO deliveredOrder;
    private OrderFeignClient.OrderStatusDTO pendingOrder;

    @BeforeEach
    void setUp() {
        reviewDTO = ReviewDTO.builder()
                .orderId(50L)
                .customerId(1L)
                .targetId(3L)
                .targetType(TargetType.RESTAURANT)
                .rating(5)
                .comment("Excelente servicio")
                .build();

        savedReview = Review.builder()
                .id(1L)
                .orderId(50L)
                .customerId(1L)
                .targetId(3L)
                .targetType(TargetType.RESTAURANT)
                .rating(5)
                .comment("Excelente servicio")
                .build();

        deliveredOrder = new OrderFeignClient.OrderStatusDTO();
        deliveredOrder.setId(50L);
        deliveredOrder.setStatus("DELIVERED");

        pendingOrder = new OrderFeignClient.OrderStatusDTO();
        pendingOrder.setId(50L);
        pendingOrder.setStatus("PENDING");
    }

    // ────────── create ──────────

    @Test
    @DisplayName("create - pedido DELIVERED y sin reseña previa guarda la reseña")
    void create_pedidoDelivered_guardaResena() {
        when(orderClient.getOrder(50L)).thenReturn(deliveredOrder);
        when(reviewRepo.existsByOrderIdAndCustomerIdAndTargetType(50L, 1L, TargetType.RESTAURANT))
                .thenReturn(false);
        when(reviewRepo.save(any(Review.class))).thenReturn(savedReview);

        ReviewResponseDTO result = reviewService.create(reviewDTO);

        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals("Excelente servicio", result.getComment());
        verify(reviewRepo, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("create - pedido NO DELIVERED lanza BusinessException")
    void create_pedidoNoDelivered_lanzaBusinessException() {
        when(orderClient.getOrder(50L)).thenReturn(pendingOrder);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reviewService.create(reviewDTO));
        assertTrue(ex.getMessage().contains("DELIVERED"));
        verify(reviewRepo, never()).save(any());
    }

    @Test
    @DisplayName("create - reseña duplicada para misma orden y tipo lanza BusinessException")
    void create_resenaDuplicada_lanzaBusinessException() {
        when(orderClient.getOrder(50L)).thenReturn(deliveredOrder);
        when(reviewRepo.existsByOrderIdAndCustomerIdAndTargetType(50L, 1L, TargetType.RESTAURANT))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reviewService.create(reviewDTO));
        assertTrue(ex.getMessage().contains("RESTAURANT"));
        verify(reviewRepo, never()).save(any());
    }

    @Test
    @DisplayName("create - rating entre 1 y 5 se guarda correctamente")
    void create_ratingSeGuardaCorrectamente() {
        reviewDTO.setRating(4);
        Review review4 = Review.builder().id(2L).orderId(50L).customerId(1L)
                .targetId(3L).targetType(TargetType.RESTAURANT).rating(4).build();
        when(orderClient.getOrder(50L)).thenReturn(deliveredOrder);
        when(reviewRepo.existsByOrderIdAndCustomerIdAndTargetType(any(), any(), any()))
                .thenReturn(false);
        when(reviewRepo.save(any(Review.class))).thenReturn(review4);

        ReviewResponseDTO result = reviewService.create(reviewDTO);

        assertEquals(4, result.getRating());
    }

    @Test
    @DisplayName("create - reseña para AGENT se guarda correctamente")
    void create_resenaParaAgent_seGuardaCorrectamente() {
        reviewDTO.setTargetType(TargetType.AGENT);
        Review agentReview = Review.builder().id(3L).orderId(50L).customerId(1L)
                .targetId(3L).targetType(TargetType.AGENT).rating(5).build();
        when(orderClient.getOrder(50L)).thenReturn(deliveredOrder);
        when(reviewRepo.existsByOrderIdAndCustomerIdAndTargetType(50L, 1L, TargetType.AGENT))
                .thenReturn(false);
        when(reviewRepo.save(any(Review.class))).thenReturn(agentReview);

        ReviewResponseDTO result = reviewService.create(reviewDTO);

        assertEquals(TargetType.AGENT, result.getTargetType());
    }

    // ────────── getByRestaurant ──────────

    @Test
    @DisplayName("getByRestaurant - retorna reseñas del restaurante")
    void getByRestaurant_retornaResenas() {
        when(reviewRepo.findByTargetIdAndTargetType(3L, TargetType.RESTAURANT))
                .thenReturn(List.of(savedReview));

        List<ReviewResponseDTO> result = reviewService.getByRestaurant(3L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getRating());
    }

    @Test
    @DisplayName("getByRestaurant - restaurante sin reseñas retorna lista vacía")
    void getByRestaurant_sinResenas_retornaListaVacia() {
        when(reviewRepo.findByTargetIdAndTargetType(99L, TargetType.RESTAURANT))
                .thenReturn(Collections.emptyList());

        List<ReviewResponseDTO> result = reviewService.getByRestaurant(99L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ────────── getByAgent ──────────

    @Test
    @DisplayName("getByAgent - retorna reseñas del repartidor")
    void getByAgent_retornaResenas() {
        Review agentReview = Review.builder().id(4L).orderId(50L).customerId(1L)
                .targetId(5L).targetType(TargetType.AGENT).rating(4).comment("Rápido").build();
        when(reviewRepo.findByTargetIdAndTargetType(5L, TargetType.AGENT))
                .thenReturn(List.of(agentReview));

        List<ReviewResponseDTO> result = reviewService.getByAgent(5L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TargetType.AGENT, result.get(0).getTargetType());
    }

    @Test
    @DisplayName("getByAgent - repartidor sin reseñas retorna lista vacía")
    void getByAgent_sinResenas_retornaListaVacia() {
        when(reviewRepo.findByTargetIdAndTargetType(99L, TargetType.AGENT))
                .thenReturn(Collections.emptyList());

        List<ReviewResponseDTO> result = reviewService.getByAgent(99L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ────────── getAvgRating ──────────

    @Test
    @DisplayName("getAvgRating - retorna promedio de calificaciones del restaurante")
    void getAvgRating_retornaPromedio() {
        when(reviewRepo.findAverageRating(3L, TargetType.RESTAURANT)).thenReturn(4.5);

        Double avg = reviewService.getAvgRating(3L, TargetType.RESTAURANT);

        assertNotNull(avg);
        assertEquals(4.5, avg);
    }

    @Test
    @DisplayName("getAvgRating - restaurante sin reseñas retorna null")
    void getAvgRating_sinResenas_retornaNull() {
        when(reviewRepo.findAverageRating(99L, TargetType.RESTAURANT)).thenReturn(null);

        Double avg = reviewService.getAvgRating(99L, TargetType.RESTAURANT);

        assertNull(avg);
    }

    @Test
    @DisplayName("getAvgRating - funciona también para tipo AGENT")
    void getAvgRating_funcionaParaAgent() {
        when(reviewRepo.findAverageRating(5L, TargetType.AGENT)).thenReturn(3.8);

        Double avg = reviewService.getAvgRating(5L, TargetType.AGENT);

        assertEquals(3.8, avg);
    }
}
