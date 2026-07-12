package com.foodmarket.search.controller;

import com.foodmarket.search.dto.RestaurantIndexRequestDTO;
import com.foodmarket.search.dto.RestaurantIndexResponseDTO;
import com.foodmarket.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller del buscador (patrón CSR): solo maneja las solicitudes REST
 * y delega la lógica de negocio a SearchService.
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "Búsqueda", description = "Búsqueda e indexación de restaurantes (ruta pública, sin JWT)")
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "Buscar restaurantes",
            description = "Permite buscar restaurantes por nombre, zona o categoría. Si no se envía ningún parámetro retorna todos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resultados de búsqueda retornados exitosamente")
    })
    @GetMapping("/restaurants")
    public ResponseEntity<List<RestaurantIndexResponseDTO>> search(
            @Parameter(description = "Nombre del restaurante (búsqueda parcial)") @RequestParam(required = false) String name,
            @Parameter(description = "Zona geográfica") @RequestParam(required = false) String zone,
            @Parameter(description = "Categoría gastronómica") @RequestParam(required = false) String category) {
        return ResponseEntity.ok(searchService.search(name, zone, category));
    }

    @Operation(summary = "Indexar restaurante",
            description = "Crea o actualiza el índice de búsqueda de un restaurante. Lo llama internamente el ms-restaurant.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Restaurante indexado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    @PostMapping("/restaurants/index")
    public ResponseEntity<RestaurantIndexResponseDTO> indexRestaurant(
            @Parameter(description = "Datos del restaurante a indexar") @Valid @RequestBody RestaurantIndexRequestDTO entry) {
        return ResponseEntity.status(HttpStatus.CREATED).body(searchService.indexRestaurant(entry));
    }
}
