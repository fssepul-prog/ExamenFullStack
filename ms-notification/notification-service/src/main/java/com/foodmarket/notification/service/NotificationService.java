package com.foodmarket.notification.service;

import com.foodmarket.notification.dto.NotificationResponseDTO;
import com.foodmarket.notification.exception.ResourceNotFoundException;
import com.foodmarket.notification.model.Notification;
import com.foodmarket.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notifRepo;

    /**
     * Crea una notificación. Uso interno: lo invoca el consumidor de eventos Kafka,
     * por lo que trabaja directamente con la entidad.
     */
    public Notification create(Long recipientId, String type, String message) {
        Notification notif = Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .message(message)
                .read(false)
                .build();
        Notification saved = notifRepo.save(notif);
        log.info("[NOTIFICATION] Notificacion creada para usuario {}: {}", recipientId, type);
        return saved;
    }

    public List<NotificationResponseDTO> getByUser(Long userId) {
        log.info("[NOTIFICATION] Consultando notificaciones para usuario {}", userId);
        return notifRepo.findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    public List<NotificationResponseDTO> getUnread(Long userId) {
        log.info("[NOTIFICATION] Consultando no leidas para usuario {}", userId);
        return notifRepo.findByRecipientIdAndReadFalse(userId)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Marca una notificación como leída.
     * @throws ResourceNotFoundException si la notificación no existe (retorna 404 vía handler).
     */
    public void markAsRead(Long id) {
        Notification notif = notifRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notificación con id " + id + " no encontrada"));
        notif.setRead(true);
        notifRepo.save(notif);
        log.info("[NOTIFICATION] Notificacion {} marcada como leida", id);
    }

    private NotificationResponseDTO toResponse(Notification n) {
        return NotificationResponseDTO.builder()
                .id(n.getId())
                .recipientId(n.getRecipientId())
                .type(n.getType())
                .message(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
