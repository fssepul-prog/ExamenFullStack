package com.foodmarket.search.dto;

import lombok.*;

/**
 * DTO de salida del buscador. Evita exponer la entidad JPA directamente en la API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantIndexResponseDTO {
    private Long id;
    private Long restaurantId;
    private String name;
    private String category;
    private String zone;
    private String status;
    private Double avgRating;
}
