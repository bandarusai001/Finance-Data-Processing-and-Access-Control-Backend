package com.finance.dashboard.service;

import com.finance.dashboard.dto.request.CreateUserRequest;
import com.finance.dashboard.dto.request.UpdateUserRequest;
import com.finance.dashboard.dto.response.UserResponse;
import com.finance.dashboard.exception.BusinessException;
import com.finance.dashboard.exception.ResourceNotFoundException;
import com.finance.dashboard.model.User;
import com.finance.dashboard.model.enums.Role;
import com.finance.dashboard.model.enums.UserStatus;
import com.finance.dashboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock UserRepository  userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserService userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded_pass")
                .fullName("Test User")
                .role(Role.VIEWER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    // ── createUser ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createUser saves user when username and email are unique")
    void createUser_success() {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("newuser");
        req.setEmail("new@example.com");
        req.setPassword("password123");
        req.setFullName("New User");
        req.setRole(Role.ANALYST);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });

        UserResponse response = userService.createUser(req);

        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getRole()).isEqualTo(Role.ANALYST);
        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser throws BusinessException when username already exists")
    void createUser_duplicateUsername() {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("testuser");
        req.setEmail("other@example.com");
        req.setPassword("pass");
        req.setFullName("Someone");
        req.setRole(Role.VIEWER);

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("testuser");
    }

    @Test
    @DisplayName("createUser throws BusinessException when email already registered")
    void createUser_duplicateEmail() {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("brandnew");
        req.setEmail("test@example.com");
        req.setPassword("pass");
        req.setFullName("Someone");
        req.setRole(Role.VIEWER);

        when(userRepository.existsByUsername("brandnew")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("test@example.com");
    }

    // ── getUserById ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserById returns user when found")
    void getUserById_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        UserResponse response = userService.getUserById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("getUserById throws ResourceNotFoundException when not found")
    void getUserById_notFound() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    // ── updateUser ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateUser applies only non-null fields")
    void updateUser_partialUpdate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setRole(Role.ANALYST);   // only update role

        UserResponse response = userService.updateUser(1L, req);

        assertThat(response.getRole()).isEqualTo(Role.ANALYST);
        assertThat(response.getFullName()).isEqualTo("Test User");  // unchanged
        assertThat(response.getEmail()).isEqualTo("test@example.com");  // unchanged
    }

    @Test
    @DisplayName("updateUser encodes password when password field is provided")
    void updateUser_encodesPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("newpass")).thenReturn("newly_encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setPassword("newpass");

        userService.updateUser(1L, req);

        assertThat(existingUser.getPassword()).isEqualTo("newly_encoded");
        verify(passwordEncoder).encode("newpass");
    }

    // ── deleteUser ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteUser calls repository.delete")
    void deleteUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        doNothing().when(userRepository).delete(existingUser);

        userService.deleteUser(1L);

        verify(userRepository).delete(existingUser);
    }
}
