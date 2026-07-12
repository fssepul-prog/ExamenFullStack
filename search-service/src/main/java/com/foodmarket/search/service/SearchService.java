package com.foodmarket.search.service;

import com.foodmarket.search.dto.RestaurantIndexRequestDTO;
import com.foodmarket.search.dto.RestaurantIndexResponseDTO;
import com.foodmarket.search.model.RestaurantIndex;
import com.foodmarket.search.repository.RestaurantIndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Capa de servicio del buscador (patrón CSR).
 * Contiene la lógica de negocio de búsqueda e indexación,
 * dejando al controller solo el manejo de solicitudes REST.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final RestaurantIndexRepository indexRepo;

    /**
     * Busca restaurantes por nombre, zona o categoría (en ese orden de prioridad).
     * Si no se entrega ningún criterio, retorna todos los restaurantes indexados.
     */
    public List<RestaurantIndexResponseDTO> search(String name, String zone, String category) {
        log.info("[SEARCH] Busqueda iniciada: name={} zone={} category={}", name, zone, category);

        List<RestaurantIndex> result;
        if (name != null) {
            result = indexRepo.searchByName(name);
            log.info("[SEARCH] Por nombre '{}': {} resultados", name, result.size());
        } else if (zone != null) {
            result = indexRepo.findByZoneOrderByRating(zone);
            log.info("[SEARCH] Por zona '{}': {} resultados", zone, result.size());
        } else if (category != null) {
            result = indexRepo.findByCategoryAndStatus(category, "OPEN");
            log.info("[SEARCH] Por categoria '{}': {} resultados", category, result.size());
        } else {
            result = indexRepo.findAll();
            log.info("[SEARCH] Busqueda general: {} restaurantes", result.size());
        }
        return result.stream().map(this::toResponse).toList();
    }

    /**
     * Crea o actualiza el índice de búsqueda de un restaurante.
     * Si el restaurantId ya existe, actualiza sus datos; si no, crea una nueva entrada.
     */
    public RestaurantIndexResponseDTO indexRestaurant(RestaurantIndexRequestDTO dto) {
        log.info("[SEARCH] Indexando restaurante: {} (id={})", dto.getName(), dto.getRestaurantId());

        RestaurantIndex saved = indexRepo.findByRestaurantId(dto.getRestaurantId())
                .map(existing -> {
                    existing.setName(dto.getName());
                    existing.setCategory(dto.getCategory());
                    existing.setZone(dto.getZone());
                    existing.setStatus(dto.getStatus());
                    log.info("[SEARCH] Restaurante {} ya indexado, actualizando datos", dto.getRestaurantId());
                    return indexRepo.save(existing);
                })
                .orElseGet(() -> {
                    log.info("[SEARCH] Restaurante {} no indexado, creando entrada nueva", dto.getRestaurantId());
                    return indexRepo.save(toEntity(dto));
                });

        log.info("[SEARCH] Restaurante indexado con id={}", saved.getId());
        return toResponse(saved);
    }

    private RestaurantIndex toEntity(RestaurantIndexRequestDTO dto) {
        return RestaurantIndex.builder()
                .restaurantId(dto.getRestaurantId())
                .name(dto.getName())
                .category(dto.getCategory())
                .zone(dto.getZone())
                .status(dto.getStatus())
                .avgRating(0.0)
                .build();
    }

    private RestaurantIndexResponseDTO toResponse(RestaurantIndex entity) {
        return RestaurantIndexResponseDTO.builder()
                .id(entity.getId())
                .restaurantId(entity.getRestaurantId())
                .name(entity.getName())
                .category(entity.getCategory())
                .zone(entity.getZone())
                .status(entity.getStatus())
                .avgRating(entity.getAvgRating())
                .build();
    }
}
