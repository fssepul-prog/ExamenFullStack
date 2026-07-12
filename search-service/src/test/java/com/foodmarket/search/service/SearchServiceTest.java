package com.foodmarket.search.service;

import com.foodmarket.search.dto.RestaurantIndexRequestDTO;
import com.foodmarket.search.dto.RestaurantIndexResponseDTO;
import com.foodmarket.search.model.RestaurantIndex;
import com.foodmarket.search.repository.RestaurantIndexRepository;
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
@DisplayName("SearchService - Pruebas unitarias (lógica de búsqueda e indexación)")
class SearchServiceTest {

    @Mock
    private RestaurantIndexRepository indexRepo;

    @InjectMocks
    private SearchService searchService;

    private RestaurantIndex restaurantA;
    private RestaurantIndex restaurantB;

    @BeforeEach
    void setUp() {
        restaurantA = RestaurantIndex.builder()
                .id(1L)
                .restaurantId(10L)
                .name("Pizza Express")
                .category("ITALIANA")
                .zone("PROVIDENCIA")
                .status("OPEN")
                .avgRating(4.5)
                .build();

        restaurantB = RestaurantIndex.builder()
                .id(2L)
                .restaurantId(20L)
                .name("Sushi Bar")
                .category("JAPONESA")
                .zone("LAS_CONDES")
                .status("OPEN")
                .avgRating(4.8)
                .build();
    }

    // ────────── search por nombre ──────────

    @Test
    @DisplayName("search - por nombre delega a searchByName y mapea a DTO")
    void search_porNombre_delegaASearchByName() {
        // Given
        when(indexRepo.searchByName("pizza")).thenReturn(List.of(restaurantA));

        // When
        List<RestaurantIndexResponseDTO> result = searchService.search("pizza", null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Pizza Express", result.get(0).getName());
        assertEquals(10L, result.get(0).getRestaurantId());
        verify(indexRepo).searchByName("pizza");
        verify(indexRepo, never()).findByZoneOrderByRating(any());
        verify(indexRepo, never()).findByCategoryAndStatus(any(), any());
        verify(indexRepo, never()).findAll();
    }

    @Test
    @DisplayName("search - por nombre sin resultados retorna lista vacía")
    void search_porNombreSinResultados_retornaListaVacia() {
        // Given
        when(indexRepo.searchByName("inexistente")).thenReturn(Collections.emptyList());

        // When
        List<RestaurantIndexResponseDTO> result = searchService.search("inexistente", null, null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("search - el nombre tiene prioridad sobre zona y categoría")
    void search_nombreTienePrioridad() {
        // Given
        when(indexRepo.searchByName("sushi")).thenReturn(List.of(restaurantB));

        // When
        searchService.search("sushi", "PROVIDENCIA", "ITALIANA");

        // Then
        verify(indexRepo).searchByName("sushi");
        verify(indexRepo, never()).findByZoneOrderByRating(any());
        verify(indexRepo, never()).findByCategoryAndStatus(any(), any());
    }

    // ────────── search por zona ──────────

    @Test
    @DisplayName("search - por zona delega a findByZoneOrderByRating")
    void search_porZona_delegaAFindByZone() {
        // Given
        when(indexRepo.findByZoneOrderByRating("LAS_CONDES")).thenReturn(List.of(restaurantB));

        // When
        List<RestaurantIndexResponseDTO> result = searchService.search(null, "LAS_CONDES", null);

        // Then
        assertEquals(1, result.size());
        assertEquals("Sushi Bar", result.get(0).getName());
        verify(indexRepo).findByZoneOrderByRating("LAS_CONDES");
        verify(indexRepo, never()).searchByName(any());
        verify(indexRepo, never()).findAll();
    }

    @Test
    @DisplayName("search - la zona tiene prioridad sobre la categoría")
    void search_zonaTienePrioridadSobreCategoria() {
        // Given
        when(indexRepo.findByZoneOrderByRating("PROVIDENCIA")).thenReturn(List.of(restaurantA));

        // When
        searchService.search(null, "PROVIDENCIA", "JAPONESA");

        // Then
        verify(indexRepo).findByZoneOrderByRating("PROVIDENCIA");
        verify(indexRepo, never()).findByCategoryAndStatus(any(), any());
    }

    // ────────── search por categoría ──────────

    @Test
    @DisplayName("search - por categoría busca solo restaurantes con estado OPEN")
    void search_porCategoria_filtraPorEstadoOpen() {
        // Given
        when(indexRepo.findByCategoryAndStatus("ITALIANA", "OPEN")).thenReturn(List.of(restaurantA));

        // When
        List<RestaurantIndexResponseDTO> result = searchService.search(null, null, "ITALIANA");

        // Then
        assertEquals(1, result.size());
        assertEquals("ITALIANA", result.get(0).getCategory());
        verify(indexRepo).findByCategoryAndStatus("ITALIANA", "OPEN");
    }

    // ────────── search general ──────────

    @Test
    @DisplayName("search - sin parámetros retorna todos los restaurantes indexados")
    void search_sinParametros_retornaTodos() {
        // Given
        when(indexRepo.findAll()).thenReturn(List.of(restaurantA, restaurantB));

        // When
        List<RestaurantIndexResponseDTO> result = searchService.search(null, null, null);

        // Then
        assertEquals(2, result.size());
        verify(indexRepo).findAll();
        verify(indexRepo, never()).searchByName(any());
        verify(indexRepo, never()).findByZoneOrderByRating(any());
        verify(indexRepo, never()).findByCategoryAndStatus(any(), any());
    }

    @Test
    @DisplayName("search - el DTO de salida contiene todos los campos de la entidad")
    void search_dtoContieneTodosLosCampos() {
        // Given
        when(indexRepo.findAll()).thenReturn(List.of(restaurantA));

        // When
        RestaurantIndexResponseDTO dto = searchService.search(null, null, null).get(0);

        // Then
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getRestaurantId());
        assertEquals("Pizza Express", dto.getName());
        assertEquals("ITALIANA", dto.getCategory());
        assertEquals("PROVIDENCIA", dto.getZone());
        assertEquals("OPEN", dto.getStatus());
        assertEquals(4.5, dto.getAvgRating());
    }

    // ────────── indexRestaurant ──────────

    @Test
    @DisplayName("indexRestaurant - restaurante nuevo crea una entrada con rating inicial 0.0")
    void indexRestaurant_restauranteNuevo_creaEntrada() {
        // Given
        RestaurantIndexRequestDTO dto = RestaurantIndexRequestDTO.builder()
                .restaurantId(30L).name("Burger House").category("RAPIDA")
                .zone("ÑUÑOA").status("OPEN").build();
        when(indexRepo.findByRestaurantId(30L)).thenReturn(Optional.empty());
        when(indexRepo.save(any(RestaurantIndex.class)))
                .thenAnswer(inv -> {
                    RestaurantIndex e = inv.getArgument(0);
                    e.setId(3L);
                    return e;
                });

        // When
        RestaurantIndexResponseDTO result = searchService.indexRestaurant(dto);

        // Then
        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("Burger House", result.getName());
        assertEquals(0.0, result.getAvgRating());
        verify(indexRepo).save(argThat(e ->
                e.getRestaurantId().equals(30L) &&
                e.getName().equals("Burger House") &&
                e.getStatus().equals("OPEN")
        ));
    }

    @Test
    @DisplayName("indexRestaurant - restaurante existente actualiza sus datos sin duplicar")
    void indexRestaurant_restauranteExistente_actualizaDatos() {
        // Given
        RestaurantIndexRequestDTO dto = RestaurantIndexRequestDTO.builder()
                .restaurantId(10L).name("Pizza Express Renovada").category("ITALIANA")
                .zone("SANTIAGO_CENTRO").status("CLOSED").build();
        when(indexRepo.findByRestaurantId(10L)).thenReturn(Optional.of(restaurantA));
        when(indexRepo.save(any(RestaurantIndex.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        RestaurantIndexResponseDTO result = searchService.indexRestaurant(dto);

        // Then
        assertEquals(1L, result.getId());
        assertEquals("Pizza Express Renovada", result.getName());
        assertEquals("SANTIAGO_CENTRO", result.getZone());
        assertEquals("CLOSED", result.getStatus());
        verify(indexRepo, times(1)).save(restaurantA);
    }

    @Test
    @DisplayName("indexRestaurant - al actualizar conserva el rating acumulado del restaurante")
    void indexRestaurant_actualizacion_conservaRating() {
        // Given
        RestaurantIndexRequestDTO dto = RestaurantIndexRequestDTO.builder()
                .restaurantId(10L).name("Pizza Express").category("ITALIANA")
                .zone("PROVIDENCIA").status("OPEN").build();
        when(indexRepo.findByRestaurantId(10L)).thenReturn(Optional.of(restaurantA));
        when(indexRepo.save(any(RestaurantIndex.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        RestaurantIndexResponseDTO result = searchService.indexRestaurant(dto);

        // Then
        assertEquals(4.5, result.getAvgRating());
    }

    @Test
    @DisplayName("indexRestaurant - consulta por restaurantId exactamente una vez")
    void indexRestaurant_consultaPorRestaurantIdUnaVez() {
        // Given
        RestaurantIndexRequestDTO dto = RestaurantIndexRequestDTO.builder()
                .restaurantId(20L).name("Sushi Bar").status("OPEN").build();
        when(indexRepo.findByRestaurantId(20L)).thenReturn(Optional.of(restaurantB));
        when(indexRepo.save(any(RestaurantIndex.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        searchService.indexRestaurant(dto);

        // Then
        verify(indexRepo, times(1)).findByRestaurantId(20L);
        verify(indexRepo, never()).findAll();
    }
}
