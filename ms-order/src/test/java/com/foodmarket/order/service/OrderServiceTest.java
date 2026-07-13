package com.foodmarket.order.service;

import com.foodmarket.order.client.RestaurantFeignClient;
import com.foodmarket.order.dto.*;
import com.foodmarket.order.event.OrderEvent;
import com.foodmarket.order.exception.BusinessException;
import com.foodmarket.order.exception.ResourceNotFoundException;
import com.foodmarket.order.kafka.OrderEventProducer;
import com.foodmarket.order.model.Order;
import com.foodmarket.order.model.OrderStatus;
import com.foodmarket.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService - Pruebas unitarias")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepo;

    @Mock
    private RestaurantFeignClient restaurantClient;

    @Mock
    private OrderEventProducer eventProducer;

    @InjectMocks
    private OrderService orderService;

    private RestaurantClientDTO openRestaurant;
    private RestaurantClientDTO closedRestaurant;
    private MenuItemClientDTO availableItem;
    private MenuItemClientDTO unavailableItem;
    private Order savedOrder;
    private CreateOrderDTO createOrderDTO;

    @BeforeEach
    void setUp() {
        openRestaurant = new RestaurantClientDTO();
        openRestaurant.setId(1L);
        openRestaurant.setStatus("OPEN");
        openRestaurant.setZone("PROVIDENCIA");

        closedRestaurant = new RestaurantClientDTO();
        closedRestaurant.setId(2L);
        closedRestaurant.setStatus("CLOSED");
        closedRestaurant.setZone("PROVIDENCIA");

        availableItem = new MenuItemClientDTO();
        availableItem.setId(1L);
        availableItem.setName("Pizza Margherita");
        availableItem.setPrice(new BigDecimal("9990"));
        availableItem.setStock(5);
        availableItem.setAvailable(true);

        unavailableItem = new MenuItemClientDTO();
        unavailableItem.setId(2L);
        unavailableItem.setName("Pizza Agotada");
        unavailableItem.setPrice(new BigDecimal("9990"));
        unavailableItem.setStock(0);
        unavailableItem.setAvailable(false);

        CreateOrderDTO.OrderItemDTO itemDTO = new CreateOrderDTO.OrderItemDTO();
        itemDTO.setMenuItemId(1L);
        itemDTO.setQuantity(2);

        createOrderDTO = new CreateOrderDTO();
        createOrderDTO.setRestaurantId(1L);
        createOrderDTO.setDeliveryAddress("Av. Providencia 1234");
        createOrderDTO.setDeliveryZone("PROVIDENCIA");
        createOrderDTO.setItems(List.of(itemDTO));

        savedOrder = Order.builder()
                .id(100L)
                .customerId(1L)
                .restaurantId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("21970"))
                .deliveryFee(new BigDecimal("1990"))
                .deliveryAddress("Av. Providencia 1234")
                .deliveryZone("PROVIDENCIA")
                .items(new ArrayList<>())
                .build();
    }

    // ────────── createOrder ──────────

    @Test
    @DisplayName("createOrder - pedido válido se crea exitosamente y publica evento Kafka")
    void createOrder_pedidoValido_seCrearYPublicaEventoKafka() {
        when(restaurantClient.getRestaurant(1L)).thenReturn(openRestaurant);
        when(restaurantClient.getMenuItem(1L, 1L)).thenReturn(availableItem);
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponseDTO result = orderService.createOrder(createOrderDTO, 1L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        verify(eventProducer, times(1)).publishOrderCreated(any(OrderEvent.class));
    }

    @Test
    @DisplayName("createOrder - restaurante CERRADO lanza BusinessException")
    void createOrder_restauranteCerrado_lanzaBusinessException() {
        when(restaurantClient.getRestaurant(1L)).thenReturn(closedRestaurant);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(createOrderDTO, 1L));
        assertTrue(ex.getMessage().contains("cerrado"));
        verify(orderRepo, never()).save(any());
        verify(eventProducer, never()).publishOrderCreated(any());
    }

    @Test
    @DisplayName("createOrder - zona diferente al restaurante lanza BusinessException")
    void createOrder_zonaDistintaAlRestaurante_lanzaBusinessException() {
        createOrderDTO.setDeliveryZone("LAS_CONDES");
        when(restaurantClient.getRestaurant(1L)).thenReturn(openRestaurant);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(createOrderDTO, 1L));
        assertTrue(ex.getMessage().contains("zona"));
        verify(orderRepo, never()).save(any());
    }

    @Test
    @DisplayName("createOrder - item sin stock lanza BusinessException")
    void createOrder_itemSinStock_lanzaBusinessException() {
        when(restaurantClient.getRestaurant(1L)).thenReturn(openRestaurant);
        when(restaurantClient.getMenuItem(1L, 1L)).thenReturn(unavailableItem);

        assertThrows(BusinessException.class, () -> orderService.createOrder(createOrderDTO, 1L));
        verify(orderRepo, never()).save(any());
    }

    @Test
    @DisplayName("createOrder - el total incluye fee de delivery de $1990")
    void createOrder_elTotalIncluyeFeeDelivery() {
        when(restaurantClient.getRestaurant(1L)).thenReturn(openRestaurant);
        when(restaurantClient.getMenuItem(1L, 1L)).thenReturn(availableItem);
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponseDTO result = orderService.createOrder(createOrderDTO, 1L);

        assertEquals(new BigDecimal("1990"), result.getDeliveryFee());
    }

    @Test
    @DisplayName("createOrder - estado inicial del pedido es PENDING")
    void createOrder_estadoInicialEsPending() {
        when(restaurantClient.getRestaurant(1L)).thenReturn(openRestaurant);
        when(restaurantClient.getMenuItem(1L, 1L)).thenReturn(availableItem);
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponseDTO result = orderService.createOrder(createOrderDTO, 1L);

        assertEquals(OrderStatus.PENDING, result.getStatus());
    }

    @Test
    @DisplayName("createOrder - item no disponible (available=false) lanza BusinessException")
    void createOrder_itemNoDisponible_lanzaBusinessException() {
        MenuItemClientDTO itemNoDisponible = new MenuItemClientDTO();
        itemNoDisponible.setId(3L);
        itemNoDisponible.setName("Item no disponible");
        itemNoDisponible.setPrice(new BigDecimal("5000"));
        itemNoDisponible.setStock(3);
        itemNoDisponible.setAvailable(false);

        when(restaurantClient.getRestaurant(1L)).thenReturn(openRestaurant);
        when(restaurantClient.getMenuItem(1L, 1L)).thenReturn(itemNoDisponible);

        assertThrows(BusinessException.class, () -> orderService.createOrder(createOrderDTO, 1L));
    }

    // ────────── getById ──────────

    @Test
    @DisplayName("getById - pedido no encontrado lanza ResourceNotFoundException")
    void getById_pedidoNoEncontrado_lanzaResourceNotFoundException() {
        when(orderRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getById(999L));
    }

    @Test
    @DisplayName("getById - pedido existente retorna DTO con datos correctos")
    void getById_pedidoExistente_retornaDTO() {
        when(orderRepo.findById(100L)).thenReturn(Optional.of(savedOrder));

        OrderResponseDTO result = orderService.getById(100L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(new BigDecimal("21970"), result.getTotalAmount());
    }

    // ────────── getByCustomer ──────────

    @Test
    @DisplayName("getByCustomer - retorna lista de pedidos del cliente ordenados por fecha")
    void getByCustomer_retornaListaDePedidos() {
        Order order2 = Order.builder()
                .id(101L)
                .customerId(1L)
                .restaurantId(2L)
                .status(OrderStatus.DELIVERED)
                .totalAmount(new BigDecimal("15000"))
                .deliveryFee(new BigDecimal("1990"))
                .deliveryAddress("Otra dirección")
                .deliveryZone("PROVIDENCIA")
                .items(new ArrayList<>())
                .build();
        when(orderRepo.findByCustomerIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(savedOrder, order2));

        List<OrderResponseDTO> result = orderService.getByCustomer(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(100L, result.get(0).getId());
        assertEquals(101L, result.get(1).getId());
    }

    @Test
    @DisplayName("getByCustomer - cliente sin pedidos retorna lista vacía")
    void getByCustomer_sinPedidos_retornaListaVacia() {
        when(orderRepo.findByCustomerIdOrderByCreatedAtDesc(99L))
                .thenReturn(Collections.emptyList());

        List<OrderResponseDTO> result = orderService.getByCustomer(99L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ────────── updateStatus ──────────

    @Test
    @DisplayName("updateStatus - PENDING a CONFIRMED es transicion valida")
    void updateStatus_pendingAConfirmed_esValido() {
        savedOrder.setStatus(OrderStatus.PENDING);
        when(orderRepo.findById(100L)).thenReturn(Optional.of(savedOrder));
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponseDTO result = orderService.updateStatus(100L, OrderStatus.CONFIRMED, "ADMIN");

        assertNotNull(result);
        verify(eventProducer, times(1)).publishStatusChanged(any(OrderEvent.class));
    }

    @Test
    @DisplayName("updateStatus - CONFIRMED a PREPARING es transicion valida")
    void updateStatus_confirmedAPreparing_esValido() {
        savedOrder.setStatus(OrderStatus.CONFIRMED);
        when(orderRepo.findById(100L)).thenReturn(Optional.of(savedOrder));
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);

        orderService.updateStatus(100L, OrderStatus.PREPARING, "ADMIN");

        verify(eventProducer, times(1)).publishStatusChanged(any(OrderEvent.class));
    }

    @Test
    @DisplayName("updateStatus - PREPARING a READY es transicion valida")
    void updateStatus_preparingAReady_esValido() {
        savedOrder.setStatus(OrderStatus.PREPARING);
        when(orderRepo.findById(100L)).thenReturn(Optional.of(savedOrder));
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);

        orderService.updateStatus(100L, OrderStatus.READY, "ADMIN");

        verify(eventProducer, times(1)).publishStatusChanged(any(OrderEvent.class));
    }

    @Test
    @DisplayName("updateStatus - READY a IN_DELIVERY es transicion valida")
    void updateStatus_readyAInDelivery_esValido() {
        savedOrder.setStatus(OrderStatus.READY);
        when(orderRepo.findById(100L)).thenReturn(Optional.of(savedOrder));
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);

        orderService.updateStatus(100L, OrderStatus.IN_DELIVERY, "ADMIN");

        verify(eventProducer, times(1)).publishStatusChanged(any(OrderEvent.class));
    }

    @Test
    @DisplayName("updateStatus - PREPARING a CANCELLED solo ADMIN puede cancelar")
    void updateStatus_preparingACancelled_soloAdminPuedeCancelar() {
        savedOrder.setStatus(OrderStatus.PREPARING);
        when(orderRepo.findById(100L)).thenReturn(Optional.of(savedOrder));

        assertThrows(BusinessException.class,
                () -> orderService.updateStatus(100L, OrderStatus.CANCELLED, "CUSTOMER"));
    }

    @Test
    @DisplayName("updateStatus - PENDING a CANCELLED por CUSTOMER es valido")
    void updateStatus_pendingACancelled_porCustomer_esValido() {
        savedOrder.setStatus(OrderStatus.PENDING);
        when(orderRepo.findById(100L)).thenReturn(Optional.of(savedOrder));
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);

        assertDoesNotThrow(() -> orderService.updateStatus(100L, OrderStatus.CANCELLED, "CUSTOMER"));
        verify(eventProducer, times(1)).publishStatusChanged(any(OrderEvent.class));
    }

    @Test
    @DisplayName("updateStatus - CONFIRMED a CANCELLED por CUSTOMER es valido")
    void updateStatus_confirmedACancelled_porCustomer_esValido() {
        savedOrder.setStatus(OrderStatus.CONFIRMED);
        when(orderRepo.findById(100L)).thenReturn(Optional.of(savedOrder));
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);

        assertDoesNotThrow(() -> orderService.updateStatus(100L, OrderStatus.CANCELLED, "CUSTOMER"));
        verify(eventProducer, times(1)).publishStatusChanged(any(OrderEvent.class));
    }

    @Test
    @DisplayName("updateStatus - DELIVERED publica evento ORDER_DELIVERED en Kafka")
    void updateStatus_delivered_publicaEventoDelivered() {
        savedOrder.setStatus(OrderStatus.IN_DELIVERY);
        when(orderRepo.findById(100L)).thenReturn(Optional.of(savedOrder));
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);

        orderService.updateStatus(100L, OrderStatus.DELIVERED, "ADMIN");

        verify(eventProducer, times(1)).publishOrderDelivered(any(OrderEvent.class));
        verify(eventProducer, never()).publishStatusChanged(any());
    }

    @Test
    @DisplayName("updateStatus - transicion invalida lanza BusinessException")
    void updateStatus_transicionInvalida_lanzaBusinessException() {
        savedOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepo.findById(100L)).thenReturn(Optional.of(savedOrder));

        assertThrows(BusinessException.class,
                () -> orderService.updateStatus(100L, OrderStatus.PENDING, "ADMIN"));
    }

    @Test
    @DisplayName("updateStatus - pedido no encontrado lanza ResourceNotFoundException")
    void updateStatus_pedidoNoEncontrado_lanzaResourceNotFoundException() {
        when(orderRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.updateStatus(999L, OrderStatus.CONFIRMED, "ADMIN"));
    }

    @Test
    @DisplayName("updateStatus - CONFIRMED a CANCELLED por ADMIN es valido")
    void updateStatus_confirmedACancelled_porAdmin_esValido() {
        savedOrder.setStatus(OrderStatus.CONFIRMED);
        when(orderRepo.findById(100L)).thenReturn(Optional.of(savedOrder));
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);

        assertDoesNotThrow(() -> orderService.updateStatus(100L, OrderStatus.CANCELLED, "ADMIN"));
        verify(eventProducer, times(1)).publishStatusChanged(any(OrderEvent.class));
    }
}
