package com.foodmarket.user.service;

import com.foodmarket.user.dto.AddressDTO;
import com.foodmarket.user.dto.UserProfileDTO;
import com.foodmarket.user.exception.BusinessException;
import com.foodmarket.user.exception.ResourceNotFoundException;
import com.foodmarket.user.model.Address;
import com.foodmarket.user.model.UserProfile;
import com.foodmarket.user.repository.AddressRepository;
import com.foodmarket.user.repository.UserProfileRepository;
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
@DisplayName("UserService - Pruebas unitarias")
class UserServiceTest {

    @Mock
    private UserProfileRepository profileRepo;

    @Mock
    private AddressRepository addressRepo;

    @InjectMocks
    private UserService userService;

    private UserProfileDTO profileDTO;
    private UserProfile existingProfile;
    private AddressDTO addressDTO;
    private Address savedAddress;

    @BeforeEach
    void setUp() {
        profileDTO = UserProfileDTO.builder()
                .userId(1L)
                .fullName("Felipe Echeverría")
                .phone("+56912345678")
                .email("felipe@foodmarket.cl")
                .role("CUSTOMER")
                .build();

        existingProfile = UserProfile.builder()
                .id(10L)
                .userId(1L)
                .fullName("Felipe Viejo")
                .phone("+56900000000")
                .email("old@foodmarket.cl")
                .role("CUSTOMER")
                .build();

        addressDTO = new AddressDTO();
        addressDTO.setStreet("Av. Providencia 1234");
        addressDTO.setCity("Santiago");
        addressDTO.setZone("PROVIDENCIA");
        addressDTO.setDefaultAddr(false);

        savedAddress = Address.builder()
                .id(100L)
                .userId(1L)
                .street("Av. Providencia 1234")
                .city("Santiago")
                .zone("PROVIDENCIA")
                .active(true)
                .defaultAddr(false)
                .build();
    }

    // ────────── createProfile ──────────

    @Test
    @DisplayName("createProfile - usuario nuevo crea perfil y lo guarda")
    void createProfile_usuarioNuevo_creaYGuardaPerfil() {
        when(profileRepo.findByUserId(1L)).thenReturn(Optional.empty());
        when(profileRepo.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileDTO result = userService.createProfile(profileDTO);

        assertNotNull(result);
        assertEquals("Felipe Echeverría", result.getFullName());
        verify(profileRepo, times(1)).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("createProfile - usuario existente actualiza sus datos")
    void createProfile_usuarioExistente_actualizaDatos() {
        when(profileRepo.findByUserId(1L)).thenReturn(Optional.of(existingProfile));
        when(profileRepo.save(any(UserProfile.class))).thenReturn(existingProfile);

        UserProfileDTO result = userService.createProfile(profileDTO);

        assertNotNull(result);
        verify(profileRepo, times(1)).save(any(UserProfile.class));
        assertEquals("Felipe Echeverría", existingProfile.getFullName());
        assertEquals("felipe@foodmarket.cl", existingProfile.getEmail());
    }

    @Test
    @DisplayName("createProfile - retorna el DTO con los datos enviados")
    void createProfile_retornaDtoConDatosEnviados() {
        when(profileRepo.findByUserId(any())).thenReturn(Optional.empty());
        when(profileRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfileDTO result = userService.createProfile(profileDTO);

        assertEquals(1L, result.getUserId());
        assertEquals("CUSTOMER", result.getRole());
        assertEquals("+56912345678", result.getPhone());
    }

    // ────────── getProfile ──────────

    @Test
    @DisplayName("getProfile - perfil existente retorna DTO con datos correctos")
    void getProfile_perfilExistente_retornaDTO() {
        when(profileRepo.findByUserId(1L)).thenReturn(Optional.of(existingProfile));

        UserProfileDTO result = userService.getProfile(1L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("Felipe Viejo", result.getFullName());
        assertEquals("CUSTOMER", result.getRole());
    }

    @Test
    @DisplayName("getProfile - perfil no encontrado lanza ResourceNotFoundException")
    void getProfile_noEncontrado_lanzaResourceNotFoundException() {
        when(profileRepo.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getProfile(99L));
    }

    @Test
    @DisplayName("getProfile - retorna email del perfil correctamente")
    void getProfile_retornaEmailCorrecto() {
        when(profileRepo.findByUserId(1L)).thenReturn(Optional.of(existingProfile));

        UserProfileDTO result = userService.getProfile(1L);

        assertEquals("old@foodmarket.cl", result.getEmail());
    }

    // ────────── addAddress ──────────

    @Test
    @DisplayName("addAddress - crea dirección con active=true por defecto")
    void addAddress_creaConActivoTrue() {
        when(addressRepo.save(any(Address.class))).thenReturn(savedAddress);

        Address result = userService.addAddress(1L, addressDTO);

        assertNotNull(result);
        assertTrue(result.isActive());
        assertEquals(1L, result.getUserId());
        verify(addressRepo, times(1)).save(any(Address.class));
    }

    @Test
    @DisplayName("addAddress - calle, ciudad y zona se guardan correctamente")
    void addAddress_datosSeGuardanCorrectamente() {
        when(addressRepo.save(any(Address.class))).thenReturn(savedAddress);

        Address result = userService.addAddress(1L, addressDTO);

        assertEquals("Av. Providencia 1234", result.getStreet());
        assertEquals("Santiago", result.getCity());
        assertEquals("PROVIDENCIA", result.getZone());
    }

    @Test
    @DisplayName("addAddress - dirección default se establece según el DTO")
    void addAddress_defaultSeEstableceSegunDTO() {
        addressDTO.setDefaultAddr(true);
        Address defaultAddr = Address.builder()
                .id(101L).userId(1L).street("Otra calle").city("Santiago")
                .zone("PROVIDENCIA").active(true).defaultAddr(true).build();
        when(addressRepo.save(any(Address.class))).thenReturn(defaultAddr);

        Address result = userService.addAddress(1L, addressDTO);

        assertTrue(result.isDefaultAddr());
    }

    // ────────── getAddresses ──────────

    @Test
    @DisplayName("getAddresses - retorna solo direcciones activas del usuario")
    void getAddresses_retornaDireccionesActivas() {
        when(addressRepo.findByUserIdAndActiveTrue(1L)).thenReturn(List.of(savedAddress));

        List<Address> result = userService.getAddresses(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    @Test
    @DisplayName("getAddresses - usuario sin direcciones retorna lista vacía")
    void getAddresses_sinDirecciones_retornaListaVacia() {
        when(addressRepo.findByUserIdAndActiveTrue(99L)).thenReturn(Collections.emptyList());

        List<Address> result = userService.getAddresses(99L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ────────── deleteAddress ──────────

    @Test
    @DisplayName("deleteAddress - desactiva la dirección (soft delete)")
    void deleteAddress_desactivaDireccion() {
        when(addressRepo.findById(100L)).thenReturn(Optional.of(savedAddress));
        when(addressRepo.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.deleteAddress(100L, 1L);

        assertFalse(savedAddress.isActive());
        verify(addressRepo, times(1)).save(savedAddress);
    }

    @Test
    @DisplayName("deleteAddress - dirección no encontrada lanza ResourceNotFoundException")
    void deleteAddress_noEncontrada_lanzaResourceNotFoundException() {
        when(addressRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.deleteAddress(999L, 1L));
        verify(addressRepo, never()).save(any());
    }

    @Test
    @DisplayName("deleteAddress - usuario no autorizado lanza BusinessException")
    void deleteAddress_usuarioNoAutorizado_lanzaBusinessException() {
        when(addressRepo.findById(100L)).thenReturn(Optional.of(savedAddress));

        assertThrows(BusinessException.class,
                () -> userService.deleteAddress(100L, 99L));
        assertTrue(savedAddress.isActive(), "La dirección NO debe desactivarse si el usuario no es el dueño");
        verify(addressRepo, never()).save(any());
    }
}
