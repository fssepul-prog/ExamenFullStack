package com.foodmarket.restaurant.service;

import com.foodmarket.restaurant.dto.MenuItemDTO;
import com.foodmarket.restaurant.dto.RestaurantDTO;
import com.foodmarket.restaurant.exception.ResourceNotFoundException;
import com.foodmarket.restaurant.model.MenuItem;
import com.foodmarket.restaurant.model.Restaurant;
import com.foodmarket.restaurant.model.RestaurantStatus;
import com.foodmarket.restaurant.repository.MenuItemRepository;
import com.foodmarket.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestaurantService - Pruebas unitarias")
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepo;

    @Mock
    private MenuItemRepository menuItemRepo;

    @InjectMocks
    private RestaurantService restaurantService;

    private Restaurant restaurant;
    private MenuItem menuItem;
    private RestaurantDTO restaurantDTO;

    @BeforeEach
    void setUp() {
        restaurant = Restaurant.builder()
                .id(1L)
                .ownerId(10L)
                .name("Pizza Express")
                .category("ITALIANA")
                .zone("PROVIDENCIA")
                .status(RestaurantStatus.OPEN)
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(22, 0))
                .build();

        menuItem = MenuItem.builder()
                .id(1L)
                .restaurant(restaurant)
                .name("Pizza Margherita")
                .price(new BigDecimal("9990"))
                .stock(10)
                .available(true)
                .build();

        restaurantDTO = RestaurantDTO.builder()
                .name("Pizza Express")
                .category("ITALIANA")
                .zone("PROVIDENCIA")
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(22, 0))
                .build();
    }

    // ────────── create ──────────

    @Test
    @DisplayName("crear restaurante - se guarda con estado OPEN por defecto")
    void create_guardaRestauranteConEstadoOpen() {
        when(restaurantRepo.save(any(Restaurant.class))).thenReturn(restaurant);

        RestaurantDTO result = restaurantService.create(restaurantDTO, 10L);

        assertNotNull(result);
        assertEquals("Pizza Express", result.getName());
        assertEquals(RestaurantStatus.OPEN, result.getStatus());
        verify(restaurantRepo, times(1)).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("crear restaurante - conserva horarios de apertura y cierre")
    void create_conservaHorariosDeAperturaYCierre() {
        when(restaurantRepo.save(any(Restaurant.class))).thenReturn(restaurant);

        RestaurantDTO result = restaurantService.create(restaurantDTO, 10L);

        assertEquals(LocalTime.of(8, 0), result.getOpenTime());
        assertEquals(LocalTime.of(22, 0), result.getCloseTime());
    }

    @Test
    @DisplayName("crear restaurante - zona y categoria se guardan correctamente")
    void create_zonaYCategoriaSeGuardanCorrectamente() {
        when(restaurantRepo.save(any(Restaurant.class))).thenReturn(restaurant);

        RestaurantDTO result = restaurantService.create(restaurantDTO, 10L);

        assertEquals("PROVIDENCIA", result.getZone());
        assertEquals("ITALIANA", result.getCategory());
    }

    // ────────── getById ──────────

    @Test
    @DisplayName("getById - restaurante existente retorna DTO")
    void getById_restauranteExistente_retornaDTO() {
        when(restaurantRepo.findById(1L)).thenReturn(Optional.of(restaurant));

        RestaurantDTO result = restaurantService.getById(1L);

        assertNotNull(result);
        assertEquals("Pizza Express", result.getName());
        assertEquals("PROVIDENCIA", result.getZone());
    }

    @Test
    @DisplayName("getById - restaurante no encontrado lanza ResourceNotFoundException")
    void getById_restauranteNoEncontrado_lanzaResourceNotFoundException() {
        when(restaurantRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> restaurantService.getById(99L));
    }

    // ────────── getByZone ──────────

    @Test
    @DisplayName("getByZone - retorna solo restaurantes OPEN de la zona")
    void getByZone_retornaSoloAbiertosEnZona() {
        when(restaurantRepo.findByZoneAndStatus("PROVIDENCIA", RestaurantStatus.OPEN))
                .thenReturn(List.of(restaurant));

        List<RestaurantDTO> result = restaurantService.getByZone("PROVIDENCIA");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(RestaurantStatus.OPEN, result.get(0).getStatus());
    }

    @Test
    @DisplayName("getByZone - zona sin restaurantes retorna lista vacía")
    void getByZone_sinRestaurantes_retornaListaVacia() {
        when(restaurantRepo.findByZoneAndStatus("ZONA_INEXISTENTE", RestaurantStatus.OPEN))
                .thenReturn(Collections.emptyList());

        List<RestaurantDTO> result = restaurantService.getByZone("ZONA_INEXISTENTE");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ────────── addMenuItem ──────────

    @Test
    @DisplayName("addMenuItem - item con stock > 0 se crea disponible")
    void addMenuItem_conStockPositivo_seGuardaDisponible() {
        MenuItemDTO itemDTO = MenuItemDTO.builder()
                .name("Pizza Margherita")
                .price(new BigDecimal("9990"))
                .stock(10)
                .build();
        when(restaurantRepo.findById(1L)).thenReturn(Optional.of(restaurant));
        when(menuItemRepo.save(any(MenuItem.class))).thenReturn(menuItem);

        MenuItemDTO result = restaurantService.addMenuItem(1L, itemDTO);

        assertNotNull(result);
        assertTrue(result.isAvailable());
        verify(menuItemRepo, times(1)).save(any(MenuItem.class));
    }

    @Test
    @DisplayName("addMenuItem - item con stock 0 se crea no disponible")
    void addMenuItem_conStockCero_seGuardaNoDisponible() {
        MenuItemDTO itemDTO = MenuItemDTO.builder()
                .name("Pizza Agotada")
                .price(new BigDecimal("9990"))
                .stock(0)
                .build();
        MenuItem itemSinStock = MenuItem.builder()
                .id(2L).restaurant(restaurant).name("Pizza Agotada")
                .price(new BigDecimal("9990")).stock(0).available(false).build();
        when(restaurantRepo.findById(1L)).thenReturn(Optional.of(restaurant));
        when(menuItemRepo.save(any(MenuItem.class))).thenReturn(itemSinStock);

        MenuItemDTO result = restaurantService.addMenuItem(1L, itemDTO);

        assertFalse(result.isAvailable());
        assertEquals(0, result.getStock());
    }

    @Test
    @DisplayName("addMenuItem - restaurante no encontrado lanza ResourceNotFoundException")
    void addMenuItem_restauranteNoEncontrado_lanzaResourceNotFoundException() {
        MenuItemDTO itemDTO = MenuItemDTO.builder()
                .name("Pasta")
                .price(new BigDecimal("8990"))
                .stock(5)
                .build();
        when(restaurantRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> restaurantService.addMenuItem(99L, itemDTO));
        verify(menuItemRepo, never()).save(any());
    }

    // ────────── updateStock ──────────

    @Test
    @DisplayName("updateStock - stock > 0 mantiene item disponible")
    void updateStock_conStockPositivo_mantienDisponible() {
        when(menuItemRepo.findById(1L)).thenReturn(Optional.of(menuItem));
        when(menuItemRepo.save(any(MenuItem.class))).thenAnswer(inv -> inv.getArgument(0));

        MenuItemDTO result = restaurantService.updateStock(1L, 5);

        assertNotNull(result);
        assertEquals(5, result.getStock());
        assertTrue(result.isAvailable());
    }

    @Test
    @DisplayName("updateStock - stock 0 marca item no disponible")
    void updateStock_conStockCero_marcaNoDisponible() {
        when(menuItemRepo.findById(1L)).thenReturn(Optional.of(menuItem));
        when(menuItemRepo.save(any(MenuItem.class))).thenAnswer(inv -> inv.getArgument(0));

        MenuItemDTO result = restaurantService.updateStock(1L, 0);

        assertEquals(0, result.getStock());
        assertFalse(result.isAvailable());
    }

    @Test
    @DisplayName("updateStock - item no encontrado lanza ResourceNotFoundException")
    void updateStock_itemNoEncontrado_lanzaResourceNotFoundException() {
        when(menuItemRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> restaurantService.updateStock(99L, 5));
    }

    @Test
    @DisplayName("updateStock - stock entre 1 y 3 activa alerta de stock bajo pero item sigue disponible")
    void updateStock_stockBajo_itemSigueDisponible() {
        when(menuItemRepo.findById(1L)).thenReturn(Optional.of(menuItem));
        when(menuItemRepo.save(any(MenuItem.class))).thenAnswer(inv -> inv.getArgument(0));

        MenuItemDTO result = restaurantService.updateStock(1L, 2);

        assertEquals(2, result.getStock());
        assertTrue(result.isAvailable(), "Con stock 1-3, el item debe seguir disponible");
    }

    @Test
    @DisplayName("updateStock - stock exactamente 1 sigue disponible (límite inferior)")
    void updateStock_conStock1_sigueDisponible() {
        when(menuItemRepo.findById(1L)).thenReturn(Optional.of(menuItem));
        when(menuItemRepo.save(any(MenuItem.class))).thenAnswer(inv -> inv.getArgument(0));

        MenuItemDTO result = restaurantService.updateStock(1L, 1);

        assertEquals(1, result.getStock());
        assertTrue(result.isAvailable());
    }

    // ────────── updateStatus ──────────

    @Test
    @DisplayName("updateStatus - cambia estado del restaurante correctamente")
    void updateStatus_cambiaEstadoCorrectamente() {
        when(restaurantRepo.findById(1L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepo.save(any(Restaurant.class))).thenReturn(restaurant);

        restaurantService.updateStatus(1L, RestaurantStatus.CLOSED);

        verify(restaurantRepo).save(argThat(r -> r.getStatus() == RestaurantStatus.CLOSED));
    }

    @Test
    @DisplayName("updateStatus - restaurante no encontrado lanza ResourceNotFoundException")
    void updateStatus_restauranteNoEncontrado_lanzaResourceNotFoundException() {
        when(restaurantRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> restaurantService.updateStatus(99L, RestaurantStatus.CLOSED));
        verify(restaurantRepo, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus - puede cambiar de CLOSED a OPEN nuevamente")
    void updateStatus_deClosedAOpen_funcionaCorrectamente() {
        restaurant.setStatus(RestaurantStatus.CLOSED);
        when(restaurantRepo.findById(1L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepo.save(any(Restaurant.class))).thenReturn(restaurant);

        restaurantService.updateStatus(1L, RestaurantStatus.OPEN);

        verify(restaurantRepo).save(argThat(r -> r.getStatus() == RestaurantStatus.OPEN));
    }

    // ────────── getAvailableMenu ──────────

    @Test
    @DisplayName("getAvailableMenu - retorna solo ítems con available=true")
    void getAvailableMenu_retornaItemsDisponibles() {
        when(menuItemRepo.findByRestaurantIdAndAvailableTrue(1L)).thenReturn(List.of(menuItem));

        List<MenuItemDTO> result = restaurantService.getAvailableMenu(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isAvailable());
        assertEquals("Pizza Margherita", result.get(0).getName());
    }

    @Test
    @DisplayName("getAvailableMenu - restaurante sin ítems disponibles retorna lista vacía")
    void getAvailableMenu_sinItemsDisponibles_retornaListaVacia() {
        when(menuItemRepo.findByRestaurantIdAndAvailableTrue(1L))
                .thenReturn(Collections.emptyList());

        List<MenuItemDTO> result = restaurantService.getAvailableMenu(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getAvailableMenu - retorna precio correcto del ítem")
    void getAvailableMenu_retornaPrecioCorrectoDeLosItems() {
        when(menuItemRepo.findByRestaurantIdAndAvailableTrue(1L)).thenReturn(List.of(menuItem));

        List<MenuItemDTO> result = restaurantService.getAvailableMenu(1L);

        assertEquals(new BigDecimal("9990"), result.get(0).getPrice());
    }

    // ────────── getMenuItemById ──────────

    @Test
    @DisplayName("getMenuItemById - ítem existente retorna DTO con datos correctos")
    void getMenuItemById_itemExistente_retornaDTO() {
        when(menuItemRepo.findById(1L)).thenReturn(Optional.of(menuItem));

        MenuItemDTO result = restaurantService.getMenuItemById(1L, 1L);

        assertNotNull(result);
        assertEquals("Pizza Margherita", result.getName());
        assertEquals(new BigDecimal("9990"), result.getPrice());
        assertEquals(10, result.getStock());
        assertTrue(result.isAvailable());
    }

    @Test
    @DisplayName("getMenuItemById - ítem no encontrado lanza ResourceNotFoundException")
    void getMenuItemById_itemNoEncontrado_lanzaResourceNotFoundException() {
        when(menuItemRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> restaurantService.getMenuItemById(1L, 99L));
    }
}
