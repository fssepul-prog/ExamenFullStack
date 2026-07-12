package com.foodmarket.user.controller;

import com.foodmarket.user.dto.AddressDTO;
import com.foodmarket.user.dto.UserProfileDTO;
import com.foodmarket.user.model.Address;
import com.foodmarket.user.service.UserService;
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
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Usuarios", description = "Gestión de perfiles y direcciones de usuarios")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Crear perfil de usuario",
            description = "Crea el perfil con nombre, teléfono y foto. Debe coincidir el userId del path con el JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Perfil creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "409", description = "El usuario ya tiene perfil")
    })
    @PostMapping("/{userId}/profile")
    public ResponseEntity<UserProfileDTO> createProfile(
            @Parameter(description = "ID del usuario") @PathVariable Long userId,
            @Valid @RequestBody UserProfileDTO dto) {
        dto.setUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createProfile(dto));
    }

    @Operation(summary = "Obtener perfil de usuario")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil encontrado"),
        @ApiResponse(responseCode = "404", description = "Perfil no encontrado")
    })
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileDTO> getProfile(
            @Parameter(description = "ID del usuario") @PathVariable Long userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @Operation(summary = "Agregar dirección de entrega",
            description = "Asocia una nueva dirección de entrega al usuario")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Dirección agregada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de dirección inválidos")
    })
    @PostMapping("/{userId}/addresses")
    public ResponseEntity<Address> addAddress(
            @Parameter(description = "ID del usuario") @PathVariable Long userId,
            @Valid @RequestBody AddressDTO dto) {
        log.info("Agregando direccion para usuario {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addAddress(userId, dto));
    }

    @Operation(summary = "Obtener direcciones del usuario")
    @ApiResponse(responseCode = "200", description = "Lista de direcciones del usuario")
    @GetMapping("/{userId}/addresses")
    public ResponseEntity<List<Address>> getAddresses(
            @Parameter(description = "ID del usuario") @PathVariable Long userId) {
        return ResponseEntity.ok(userService.getAddresses(userId));
    }

    @Operation(summary = "Eliminar dirección",
            description = "Elimina una dirección de entrega del usuario")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Dirección eliminada"),
        @ApiResponse(responseCode = "404", description = "Dirección no encontrada")
    })
    @DeleteMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @Parameter(description = "ID del usuario") @PathVariable Long userId,
            @Parameter(description = "ID de la dirección") @PathVariable Long addressId) {
        userService.deleteAddress(addressId, userId);
        return ResponseEntity.noContent().build();
    }
}
