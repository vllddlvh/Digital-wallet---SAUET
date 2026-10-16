package sa.uet.digitalwallet_system.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sa.uet.digitalwallet_system.application.port.in.LoginCommand;
import sa.uet.digitalwallet_system.application.port.out.LoadUserPort;
import sa.uet.digitalwallet_system.application.port.out.PasswordVerifierPort;
import sa.uet.digitalwallet_system.application.port.out.TokenGeneratorPort;
import sa.uet.digitalwallet_system.domain.exception.InvalidCredentialsException;
import sa.uet.digitalwallet_system.domain.model.User;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private PasswordVerifierPort passwordVerifierPort;

    @Mock
    private TokenGeneratorPort tokenGeneratorPort;

    @InjectMocks
    private LoginService loginService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("john_doe")
                .passwordHash("hashed_pw")
                .fullName("John Doe")
                .email("john@example.com")
                .role("ROLE_USER")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void login_Success() {
        LoginCommand command = new LoginCommand("john_doe", "password123");
        when(loadUserPort.loadByUsername("john_doe")).thenReturn(Optional.of(user));
        when(passwordVerifierPort.matches("password123", "hashed_pw")).thenReturn(true);
        when(tokenGeneratorPort.generateToken(user)).thenReturn("mocked_jwt_token");

        String token = loginService.login(command);

        assertEquals("mocked_jwt_token", token);
        verify(tokenGeneratorPort, times(1)).generateToken(user);
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        LoginCommand command = new LoginCommand("unknown_user", "password123");
        when(loadUserPort.loadByUsername("unknown_user")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> loginService.login(command));
        verify(passwordVerifierPort, never()).matches(anyString(), anyString());
        verify(tokenGeneratorPort, never()).generateToken(any());
    }

    @Test
    void login_WrongPassword_ThrowsException() {
        LoginCommand command = new LoginCommand("john_doe", "wrong_pw");
        when(loadUserPort.loadByUsername("john_doe")).thenReturn(Optional.of(user));
        when(passwordVerifierPort.matches("wrong_pw", "hashed_pw")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> loginService.login(command));
        verify(tokenGeneratorPort, never()).generateToken(any());
    }
}
