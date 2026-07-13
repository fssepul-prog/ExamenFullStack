package com.foodmarket.notification.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de salida de notificaciones. Evita exponer la entidad JPA directamente en la API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDTO {
    private Long id;
    private Long recipientId;
    private String type;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
}
