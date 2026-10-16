package sa.uet.digitalwallet_system.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sa.uet.digitalwallet_system.application.port.in.RegisterCommand;
import sa.uet.digitalwallet_system.application.port.out.PasswordEncoderPort;
import sa.uet.digitalwallet_system.application.port.out.SaveUserPort;
import sa.uet.digitalwallet_system.application.port.out.SaveWalletPort;
import sa.uet.digitalwallet_system.domain.exception.DuplicateUserException;
import sa.uet.digitalwallet_system.domain.model.User;
import sa.uet.digitalwallet_system.domain.model.Wallet;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private SaveUserPort saveUserPort;

    @Mock
    private SaveWalletPort saveWalletPort;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @InjectMocks
    private RegisterService registerService;

    private RegisterCommand command;

    @BeforeEach
    void setUp() {
        command = new RegisterCommand("john_doe", "password123", "John Doe", "john@example.com");
    }

    @Test
    void register_Success() {
        when(saveUserPort.existsByUsername("john_doe")).thenReturn(false);
        when(saveUserPort.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoderPort.encode("password123")).thenReturn("hashed_pw");

        User savedUser = User.builder()
                .id(1L)
                .username("john_doe")
                .passwordHash("hashed_pw")
                .fullName("John Doe")
                .email("john@example.com")
                .role("ROLE_USER")
                .createdAt(LocalDateTime.now())
                .build();

        when(saveUserPort.save(any(User.class))).thenReturn(savedUser);
        when(saveWalletPort.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = registerService.register(command);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("john_doe", result.getUsername());
        verify(saveWalletPort, times(1)).save(any(Wallet.class));
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        when(saveUserPort.existsByUsername("john_doe")).thenReturn(true);

        DuplicateUserException ex = assertThrows(DuplicateUserException.class, () -> registerService.register(command));
        assertTrue(ex.getMessage().contains("username"));
        verify(saveUserPort, never()).save(any());
        verify(saveWalletPort, never()).save(any());
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        when(saveUserPort.existsByUsername("john_doe")).thenReturn(false);
        when(saveUserPort.existsByEmail("john@example.com")).thenReturn(true);

        DuplicateUserException ex = assertThrows(DuplicateUserException.class, () -> registerService.register(command));
        assertTrue(ex.getMessage().contains("email"));
        verify(saveUserPort, never()).save(any());
        verify(saveWalletPort, never()).save(any());
    }
}
