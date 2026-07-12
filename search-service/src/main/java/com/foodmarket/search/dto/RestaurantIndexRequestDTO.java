package com.foodmarket.search.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO de entrada para indexar (crear o actualizar) un restaurante en el buscador.
 * Separa la capa de transporte de la entidad JPA y aplica Bean Validation (JSR 380).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantIndexRequestDTO {

    @NotNull(message = "El id del restaurante es obligatorio")
    @Positive(message = "El id del restaurante debe ser un número positivo")
    private Long restaurantId;

    @NotBlank(message = "El nombre del restaurante es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String name;

    @Size(max = 50, message = "La categoría no puede superar los 50 caracteres")
    private String category;

    @Size(max = 100, message = "La zona no puede superar los 100 caracteres")
    private String zone;

    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "OPEN|CLOSED", message = "El estado debe ser OPEN o CLOSED")
    private String status;
}
