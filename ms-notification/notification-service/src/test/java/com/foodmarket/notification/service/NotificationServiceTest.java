package com.foodmarket.notification.service;

import com.foodmarket.notification.dto.NotificationResponseDTO;
import com.foodmarket.notification.exception.ResourceNotFoundException;
import com.foodmarket.notification.model.Notification;
import com.foodmarket.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService - Pruebas unitarias")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notifRepo;

    @InjectMocks
    private NotificationService notificationService;

    private Notification unreadNotif;
    private Notification readNotif;

    @BeforeEach
    void setUp() {
        unreadNotif = Notification.builder()
                .id(1L)
                .recipientId(10L)
                .type("ORDER_CREATED")
                .message("Tu pedido #100 fue creado exitosamente")
                .read(false)
                .build();

        readNotif = Notification.builder()
                .id(2L)
                .recipientId(10L)
                .type("ORDER_DELIVERED")
                .message("Tu pedido #100 fue entregado")
                .read(true)
                .build();
    }

    // ────────── create ──────────

    @Test
    @DisplayName("create - notificación se guarda con read=false por defecto")
    void create_guardaConReadFalse() {
        when(notifRepo.save(any(Notification.class))).thenReturn(unreadNotif);

        Notification result = notificationService.create(10L, "ORDER_CREATED",
                "Tu pedido fue creado");

        assertNotNull(result);
        assertFalse(result.isRead());
        verify(notifRepo, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("create - type y message quedan correctamente guardados")
    void create_typeYMessageSeGuardanCorrectamente() {
        when(notifRepo.save(any(Notification.class))).thenReturn(unreadNotif);

        Notification result = notificationService.create(10L, "ORDER_CREATED",
                "Tu pedido #100 fue creado exitosamente");

        assertEquals("ORDER_CREATED", result.getType());
        assertEquals("Tu pedido #100 fue creado exitosamente", result.getMessage());
    }

    @Test
    @DisplayName("create - recipientId queda asignado correctamente")
    void create_recipientIdAsignadoCorrectamente() {
        when(notifRepo.save(any(Notification.class))).thenReturn(unreadNotif);

        Notification result = notificationService.create(10L, "ORDER_CREATED", "Mensaje");

        assertEquals(10L, result.getRecipientId());
    }

    @Test
    @DisplayName("create - se invoca save exactamente una vez")
    void create_invocaSaveUnaVez() {
        when(notifRepo.save(any(Notification.class))).thenReturn(unreadNotif);

        notificationService.create(10L, "PAYMENT_COMPLETED", "Pago aprobado");

        verify(notifRepo, times(1)).save(any(Notification.class));
    }

    // ────────── getByUser ──────────

    @Test
    @DisplayName("getByUser - retorna todas las notificaciones del usuario")
    void getByUser_retornaTodasLasNotificaciones() {
        when(notifRepo.findByRecipientIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(unreadNotif, readNotif));

        List<NotificationResponseDTO> result = notificationService.getByUser(10L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("ORDER_CREATED", result.get(0).getType());
    }

    @Test
    @DisplayName("getByUser - usuario sin notificaciones retorna lista vacía")
    void getByUser_sinNotificaciones_retornaListaVacia() {
        when(notifRepo.findByRecipientIdOrderByCreatedAtDesc(99L))
                .thenReturn(Collections.emptyList());

        List<NotificationResponseDTO> result = notificationService.getByUser(99L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ────────── getUnread ──────────

    @Test
    @DisplayName("getUnread - retorna solo las notificaciones no leídas")
    void getUnread_retornaSoloNoLeidas() {
        when(notifRepo.findByRecipientIdAndReadFalse(10L))
                .thenReturn(List.of(unreadNotif));

        List<NotificationResponseDTO> result = notificationService.getUnread(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).isRead());
    }

    @Test
    @DisplayName("getUnread - usuario sin notificaciones no leídas retorna lista vacía")
    void getUnread_todasLeidas_retornaListaVacia() {
        when(notifRepo.findByRecipientIdAndReadFalse(10L))
                .thenReturn(Collections.emptyList());

        List<NotificationResponseDTO> result = notificationService.getUnread(10L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ────────── markAsRead ──────────

    @Test
    @DisplayName("markAsRead - notificación encontrada se marca como leída y se guarda")
    void markAsRead_notificacionEncontrada_marcaComoLeida() {
        when(notifRepo.findById(1L)).thenReturn(Optional.of(unreadNotif));
        when(notifRepo.save(any(Notification.class))).thenReturn(unreadNotif);

        notificationService.markAsRead(1L);

        assertTrue(unreadNotif.isRead());
        verify(notifRepo, times(1)).save(unreadNotif);
    }

    @Test
    @DisplayName("markAsRead - notificación no encontrada lanza ResourceNotFoundException (404)")
    void markAsRead_noEncontrada_lanzaResourceNotFound() {
        // Given
        when(notifRepo.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(99L));
        assertTrue(ex.getMessage().contains("99"));
        verify(notifRepo, never()).save(any());
    }

    @Test
    @DisplayName("markAsRead - notificación ya leída permanece leída")
    void markAsRead_yaLeida_permaneceLeida() {
        when(notifRepo.findById(2L)).thenReturn(Optional.of(readNotif));
        when(notifRepo.save(any(Notification.class))).thenReturn(readNotif);

        notificationService.markAsRead(2L);

        assertTrue(readNotif.isRead());
        verify(notifRepo, times(1)).save(readNotif);
    }
}
