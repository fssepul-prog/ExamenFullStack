package com.foodmarket.auth.service;

import com.foodmarket.auth.dto.AuthResponseDTO;
import com.foodmarket.auth.dto.LoginDTO;
import com.foodmarket.auth.dto.RegisterDTO;
import com.foodmarket.auth.exception.EmailAlreadyExistsException;
import com.foodmarket.auth.exception.InvalidCredentialsException;
import com.foodmarket.auth.model.Role;
import com.foodmarket.auth.model.User;
import com.foodmarket.auth.repository.UserRepository;
import com.foodmarket.auth.security.JwtUtil;
import com.foodmarket.auth.security.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Pruebas unitarias")
class AuthServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterDTO registerDTO;
    private LoginDTO loginDTO;
    private User existingUser;

    @BeforeEach
    void setUp() {
        registerDTO = new RegisterDTO();
        registerDTO.setEmail("test@foodmarket.cl");
        registerDTO.setPassword("password123");
        registerDTO.setRole(Role.CUSTOMER);

        loginDTO = new LoginDTO();
        loginDTO.setEmail("test@foodmarket.cl");
        loginDTO.setPassword("password123");

        existingUser = User.builder()
                .email("test@foodmarket.cl")
                .password(PasswordUtil.encode("password123"))
                .role(Role.CUSTOMER)
                .build();
    }

    // ────────── register ──────────

    @Test
    @DisplayName("registro exitoso - email nuevo retorna AuthResponseDTO sin token")
    void register_conEmailNuevo_retornaAuthResponseDTO() {
        when(userRepo.existsByEmail(registerDTO.getEmail())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponseDTO result = authService.register(registerDTO);

        assertNotNull(result);
        assertEquals("test@foodmarket.cl", result.getEmail());
        assertEquals("CUSTOMER", result.getRole());
        assertEquals("Registro exitoso", result.getMessage());
        verify(userRepo, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("registro falla - email ya registrado lanza EmailAlreadyExistsException")
    void register_conEmailExistente_lanzaEmailAlreadyExistsException() {
        when(userRepo.existsByEmail(registerDTO.getEmail())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(registerDTO));
        verify(userRepo, never()).save(any());
    }

    @Test
    @DisplayName("registro - la contraseña se almacena hasheada, nunca en texto plano")
    void register_laPasswordSeAlmacenaHasheada() {
        when(userRepo.existsByEmail(any())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(registerDTO);

        verify(userRepo).save(argThat(user ->
                user.getPassword() != null &&
                !user.getPassword().equals("password123") &&
                PasswordUtil.matches("password123", user.getPassword())
        ));
    }

    @Test
    @DisplayName("registro con rol RESTAURANT_OWNER retorna rol correcto")
    void register_conRolRestaurantOwner_retornaRolCorrecto() {
        registerDTO.setRole(Role.RESTAURANT_OWNER);
        User ownerUser = User.builder()
                .email("owner@foodmarket.cl")
                .password(PasswordUtil.encode("password123"))
                .role(Role.RESTAURANT_OWNER)
                .build();
        registerDTO.setEmail("owner@foodmarket.cl");
        when(userRepo.existsByEmail(any())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenReturn(ownerUser);

        AuthResponseDTO result = authService.register(registerDTO);

        assertNotNull(result);
        assertEquals("RESTAURANT_OWNER", result.getRole());
        assertNull(result.getToken(), "El registro no debe retornar token");
    }

    @Test
    @DisplayName("registro con rol ADMIN retorna rol ADMIN correctamente")
    void register_conRolAdmin_retornaRolAdmin() {
        registerDTO.setRole(Role.ADMIN);
        registerDTO.setEmail("admin@foodmarket.cl");
        User adminUser = User.builder()
                .email("admin@foodmarket.cl")
                .password(PasswordUtil.encode("password123"))
                .role(Role.ADMIN)
                .build();
        when(userRepo.existsByEmail(any())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenReturn(adminUser);

        AuthResponseDTO result = authService.register(registerDTO);

        assertEquals("ADMIN", result.getRole());
        assertEquals("Registro exitoso", result.getMessage());
    }

    @Test
    @DisplayName("registro - no retorna token JWT (solo login lo genera)")
    void register_noRetornaToken() {
        when(userRepo.existsByEmail(any())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponseDTO result = authService.register(registerDTO);

        assertNull(result.getToken());
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("registro - el email del response coincide exactamente con el ingresado")
    void register_emailEnResponseCoincideConElIngresado() {
        String email = "usuario.especial+tag@empresa.cl";
        registerDTO.setEmail(email);
        when(userRepo.existsByEmail(email)).thenReturn(false);
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponseDTO result = authService.register(registerDTO);

        assertEquals(email, result.getEmail());
    }

    // ────────── login ──────────

    @Test
    @DisplayName("login exitoso - credenciales correctas retorna token JWT")
    void login_conCredencialesCorrectas_retornaToken() {
        when(userRepo.findByEmail(loginDTO.getEmail())).thenReturn(Optional.of(existingUser));
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("jwt-token-mock");

        AuthResponseDTO result = authService.login(loginDTO);

        assertNotNull(result);
        assertEquals("jwt-token-mock", result.getToken());
        assertEquals("test@foodmarket.cl", result.getEmail());
        assertEquals("CUSTOMER", result.getRole());
        assertEquals("Login exitoso", result.getMessage());
    }

    @Test
    @DisplayName("login falla - email no existe lanza InvalidCredentialsException")
    void login_conEmailInexistente_lanzaInvalidCredentialsException() {
        when(userRepo.findByEmail(loginDTO.getEmail())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginDTO));
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("login falla - contraseña incorrecta lanza InvalidCredentialsException")
    void login_conPasswordIncorrecta_lanzaInvalidCredentialsException() {
        loginDTO.setPassword("wrongpassword");
        when(userRepo.findByEmail(loginDTO.getEmail())).thenReturn(Optional.of(existingUser));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginDTO));
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("login - se invoca generacion de token con email y rol correctos")
    void login_invocaGeneracionTokenConEmailYRol() {
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(existingUser));
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("token");

        authService.login(loginDTO);

        verify(jwtUtil).generateToken("test@foodmarket.cl", "CUSTOMER");
    }

    @Test
    @DisplayName("login - el token retornado no es nulo ni vacío")
    void login_tokenRetornadoNoEsNuloNiVacio() {
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(existingUser));
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("eyJhbGciOiJIUzI1NiJ9.test.token");

        AuthResponseDTO result = authService.login(loginDTO);

        assertNotNull(result.getToken());
        assertFalse(result.getToken().isEmpty());
    }

    @Test
    @DisplayName("login - usuario RESTAURANT_OWNER recibe su rol en el response")
    void login_usuarioRestaurantOwner_recibeRolCorrecto() {
        User ownerUser = User.builder()
                .email("owner@foodmarket.cl")
                .password(PasswordUtil.encode("pass123"))
                .role(Role.RESTAURANT_OWNER)
                .build();
        loginDTO.setEmail("owner@foodmarket.cl");
        loginDTO.setPassword("pass123");
        when(userRepo.findByEmail("owner@foodmarket.cl")).thenReturn(Optional.of(ownerUser));
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("owner-token");

        AuthResponseDTO result = authService.login(loginDTO);

        assertEquals("RESTAURANT_OWNER", result.getRole());
        verify(jwtUtil).generateToken("owner@foodmarket.cl", "RESTAURANT_OWNER");
    }

    @Test
    @DisplayName("login - mensaje de error no revela si el email existe o la password es incorrecta")
    void login_mensajeErrorEsGenerico() {
        when(userRepo.findByEmail(loginDTO.getEmail())).thenReturn(Optional.empty());

        InvalidCredentialsException ex1 = assertThrows(
                InvalidCredentialsException.class, () -> authService.login(loginDTO));

        loginDTO.setPassword("wrongpass");
        when(userRepo.findByEmail(loginDTO.getEmail())).thenReturn(Optional.of(existingUser));
        InvalidCredentialsException ex2 = assertThrows(
                InvalidCredentialsException.class, () -> authService.login(loginDTO));

        assertEquals(ex1.getMessage(), ex2.getMessage(), "Los mensajes de error deben ser idénticos");
    }
}
