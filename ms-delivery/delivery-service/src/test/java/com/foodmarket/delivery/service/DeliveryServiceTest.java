package com.foodmarket.delivery.service;

import com.foodmarket.delivery.dto.AgentDTO;
import com.foodmarket.delivery.dto.AssignDeliveryDTO;
import com.foodmarket.delivery.dto.DeliveryResponseDTO;
import com.foodmarket.delivery.exception.BusinessException;
import com.foodmarket.delivery.exception.ResourceNotFoundException;
import com.foodmarket.delivery.model.Delivery;
import com.foodmarket.delivery.model.DeliveryAgent;
import com.foodmarket.delivery.model.DeliveryStatus;
import com.foodmarket.delivery.repository.DeliveryAgentRepository;
import com.foodmarket.delivery.repository.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryService - Pruebas unitarias")
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepo;

    @Mock
    private DeliveryAgentRepository agentRepo;

    @InjectMocks
    private DeliveryService deliveryService;

    private AgentDTO agentDTO;
    private DeliveryAgent agent;
    private AssignDeliveryDTO assignDTO;
    private Delivery delivery;

    @BeforeEach
    void setUp() {
        agentDTO = AgentDTO.builder()
                .userId(5L)
                .name("Carlos Repartidor")
                .email("carlos@reparto.cl")
                .zone("PROVIDENCIA")
                .vehicleType("MOTO")
                .build();

        agent = DeliveryAgent.builder()
                .id(5L)
                .userId(5L)
                .name("Carlos Repartidor")
                .email("carlos@reparto.cl")
                .zone("PROVIDENCIA")
                .vehicleType("MOTO")
                .active(true)
                .build();

        assignDTO = new AssignDeliveryDTO(200L, "PROVIDENCIA");

        delivery = Delivery.builder()
                .id(1L)
                .orderId(200L)
                .agent(agent)
                .status(DeliveryStatus.ASSIGNED)
                .build();
    }

    // ────────── registerAgent ──────────

    @Test
    @DisplayName("registerAgent - crea repartidor con active=true y retorna DTO con nombre")
    void registerAgent_creaConActivoTrue() {
        when(agentRepo.save(any(DeliveryAgent.class))).thenReturn(agent);

        DeliveryResponseDTO result = deliveryService.registerAgent(agentDTO);

        assertNotNull(result);
        assertEquals("Carlos Repartidor", result.getAgentName());
        assertEquals("PROVIDENCIA", result.getAgentZone());
        verify(agentRepo, times(1)).save(any(DeliveryAgent.class));
    }

    @Test
    @DisplayName("registerAgent - zona del repartidor queda asignada en el DTO")
    void registerAgent_zonaCorrectaEnDTO() {
        when(agentRepo.save(any(DeliveryAgent.class))).thenReturn(agent);

        DeliveryResponseDTO result = deliveryService.registerAgent(agentDTO);

        assertEquals("PROVIDENCIA", result.getAgentZone());
    }

    @Test
    @DisplayName("registerAgent - el agente se guarda con tipo de vehículo correcto")
    void registerAgent_vehiculoTipoGuardadoCorrectamente() {
        when(agentRepo.save(any(DeliveryAgent.class))).thenReturn(agent);

        deliveryService.registerAgent(agentDTO);

        verify(agentRepo).save(argThat(a -> "MOTO".equals(a.getVehicleType())));
    }

    // ────────── assign ──────────

    @Test
    @DisplayName("assign - repartidor disponible en zona asigna entrega")
    void assign_repartidorDisponible_asignaEntrega() {
        when(agentRepo.findFirstByZoneAndActiveTrue("PROVIDENCIA")).thenReturn(Optional.of(agent));
        when(deliveryRepo.save(any(Delivery.class))).thenReturn(delivery);

        DeliveryResponseDTO result = deliveryService.assign(assignDTO);

        assertNotNull(result);
        assertEquals(200L, result.getOrderId());
        assertEquals(DeliveryStatus.ASSIGNED, result.getStatus());
        verify(deliveryRepo, times(1)).save(any(Delivery.class));
    }

    @Test
    @DisplayName("assign - sin repartidores en zona lanza BusinessException")
    void assign_sinRepartidores_lanzaBusinessException() {
        when(agentRepo.findFirstByZoneAndActiveTrue("ZONA_VACIA")).thenReturn(Optional.empty());
        assignDTO.setZone("ZONA_VACIA");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deliveryService.assign(assignDTO));
        assertTrue(ex.getMessage().contains("ZONA_VACIA"));
        verify(deliveryRepo, never()).save(any());
    }

    @Test
    @DisplayName("assign - la entrega se crea con estado ASSIGNED inicialmente")
    void assign_estadoInicialEsAssigned() {
        when(agentRepo.findFirstByZoneAndActiveTrue("PROVIDENCIA")).thenReturn(Optional.of(agent));
        when(deliveryRepo.save(any(Delivery.class))).thenReturn(delivery);

        DeliveryResponseDTO result = deliveryService.assign(assignDTO);

        assertEquals(DeliveryStatus.ASSIGNED, result.getStatus());
    }

    // ────────── updateStatus ──────────

    @Test
    @DisplayName("updateStatus - actualiza estado de la entrega correctamente")
    void updateStatus_actualizaEstado() {
        when(deliveryRepo.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepo.save(any(Delivery.class))).thenReturn(delivery);

        DeliveryResponseDTO result = deliveryService.updateStatus(1L, DeliveryStatus.PICKED_UP);

        assertNotNull(result);
        assertEquals(DeliveryStatus.PICKED_UP, delivery.getStatus());
        verify(deliveryRepo, times(1)).save(delivery);
    }

    @Test
    @DisplayName("updateStatus - estado DELIVERED establece deliveredAt")
    void updateStatus_delivered_estableceDeliveredAt() {
        when(deliveryRepo.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepo.save(any(Delivery.class))).thenReturn(delivery);

        deliveryService.updateStatus(1L, DeliveryStatus.DELIVERED);

        assertNotNull(delivery.getDeliveredAt(), "deliveredAt debe setearse al entregar");
        assertEquals(DeliveryStatus.DELIVERED, delivery.getStatus());
    }

    @Test
    @DisplayName("updateStatus - estados no-DELIVERED no establecen deliveredAt")
    void updateStatus_noDelivered_noEstableceDeliveredAt() {
        when(deliveryRepo.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepo.save(any(Delivery.class))).thenReturn(delivery);

        deliveryService.updateStatus(1L, DeliveryStatus.HEADING_TO_RESTAURANT);

        assertNull(delivery.getDeliveredAt(), "deliveredAt solo se debe setear al estado DELIVERED");
    }

    @Test
    @DisplayName("updateStatus - entrega no encontrada lanza ResourceNotFoundException")
    void updateStatus_noEncontrada_lanzaResourceNotFoundException() {
        when(deliveryRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> deliveryService.updateStatus(99L, DeliveryStatus.DELIVERED));
        verify(deliveryRepo, never()).save(any());
    }

    // ────────── getByOrder ──────────

    @Test
    @DisplayName("getByOrder - entrega encontrada retorna DTO con orderId y estado")
    void getByOrder_encontrada_retornaDTO() {
        when(deliveryRepo.findByOrderId(200L)).thenReturn(Optional.of(delivery));

        DeliveryResponseDTO result = deliveryService.getByOrder(200L);

        assertNotNull(result);
        assertEquals(200L, result.getOrderId());
        assertEquals("Carlos Repartidor", result.getAgentName());
    }

    @Test
    @DisplayName("getByOrder - pedido sin entrega lanza ResourceNotFoundException")
    void getByOrder_sinEntrega_lanzaResourceNotFoundException() {
        when(deliveryRepo.findByOrderId(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> deliveryService.getByOrder(999L));
    }
}
