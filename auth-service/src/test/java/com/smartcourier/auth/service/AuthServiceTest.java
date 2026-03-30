package com.smartcourier.auth.service;

import com.smartcourier.auth.config.JwtUtil;
import com.smartcourier.auth.dto.*;
import com.smartcourier.auth.entity.Role;
import com.smartcourier.auth.entity.User;
import com.smartcourier.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {

        signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setFullName("Test User");
        signupRequest.setPhone("1234567890");
        signupRequest.setRole("CUSTOMER");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .fullName("Test User")
                .role(Role.CUSTOMER)
                .build();
    }

    @Test
    void signup_Success() {

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(userCaptor.capture())).thenReturn(testUser);
        when(jwtUtil.generateToken("testuser", "CUSTOMER")).thenReturn("jwt-token");

        AuthResponse response = authService.signup(signupRequest);

        User savedUser = userCaptor.getValue();

        assertNotNull(savedUser);
        assertEquals("testuser", savedUser.getUsername());
        assertEquals(Role.CUSTOMER, savedUser.getRole());

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("CUSTOMER", response.getRole());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void signup_UsernameExists_ThrowsException() {

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.signup(signupRequest)
        );

        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    void signup_EmailExists_ThrowsException() {

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.signup(signupRequest)
        );

        assertEquals("Email already exists", exception.getMessage());
    }

    @Test
    void login_Success() {

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        when(passwordEncoder.matches("password123", "encodedPassword"))
                .thenReturn(true);

        when(jwtUtil.generateToken("testuser", "CUSTOMER"))
                .thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("Login successful", response.getMessage());
    }

    @Test
    void login_InvalidUsername_ThrowsException() {

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void login_InvalidPassword_ThrowsException() {

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        when(passwordEncoder.matches("password123", "encodedPassword"))
                .thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void getAllUsers_Success() {

        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<UserResponseDTO> users = authService.getAllUsers();

        assertEquals(1, users.size());
        assertEquals("testuser", users.get(0).getUsername());
        assertEquals(Role.CUSTOMER, users.get(0).getRole());
    }
}