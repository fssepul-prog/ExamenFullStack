package com.foodmarket.review.controller;

import com.foodmarket.review.dto.ReviewDTO;
import com.foodmarket.review.dto.ReviewResponseDTO;
import com.foodmarket.review.model.TargetType;
import com.foodmarket.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reseñas", description = "Reseñas y calificaciones de restaurantes y repartidores")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Crear reseña",
            description = "El cliente califica un restaurante o repartidor. Requiere que el pedido esté en estado DELIVERED. Rating: 1-5 estrellas.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reseña creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o pedido no entregado"),
        @ApiResponse(responseCode = "409", description = "Ya existe una reseña para este pedido y objetivo")
    })
    @PostMapping
    public ResponseEntity<ReviewResponseDTO> create(@Valid @RequestBody ReviewDTO dto) {
        log.info("Creando resena para orden {}", dto.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(dto));
    }

    @Operation(summary = "Obtener reseñas de un restaurante")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de reseñas del restaurante")
    })
    @GetMapping("/restaurant/{id}")
    public ResponseEntity<List<ReviewResponseDTO>> getByRestaurant(
            @Parameter(description = "ID del restaurante") @PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getByRestaurant(id));
    }

    @Operation(summary = "Obtener reseñas de un repartidor")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de reseñas del repartidor")
    })
    @GetMapping("/agent/{id}")
    public ResponseEntity<List<ReviewResponseDTO>> getByAgent(
            @Parameter(description = "ID del repartidor") @PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getByAgent(id));
    }

    @Operation(summary = "Obtener calificación promedio de un restaurante")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Calificación promedio (1.0 - 5.0)")
    })
    @GetMapping("/restaurant/{id}/average")
    public ResponseEntity<Double> getAvgRestaurant(
            @Parameter(description = "ID del restaurante") @PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getAvgRating(id, TargetType.RESTAURANT));
    }
}
