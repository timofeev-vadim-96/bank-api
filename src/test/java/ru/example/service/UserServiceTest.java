package ru.example.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.example.dao.UserRepository;
import ru.example.dto.AuthenticationRequest;
import ru.example.dto.TransferDto;
import ru.example.model.CustomUser;
import ru.example.util.Role;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {
    private static UserService service;
    private static UserRepository dao;
    private static PasswordEncoder passwordEncoder;
    private static final String TEST_LOGIN = "testLogin";

    @BeforeAll
    public static void setUp(){
        dao = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserService(dao, passwordEncoder);
    }

    @Test
    void registerNewUser() {
        AuthenticationRequest request = new AuthenticationRequest(TEST_LOGIN, "testPassword");
        when(dao.registerNewUser(any(CustomUser.class)))
                .thenReturn(Optional.of(new CustomUser(TEST_LOGIN, "encodedTestPassword", Role.USER)));
        when(passwordEncoder.encode(anyString())).thenReturn(anyString());

        Optional<CustomUser> user = service.registerNewUser(request);

        assertTrue(user.isPresent());
        assertEquals("testLogin", user.get().getLogin());
        verify(passwordEncoder, times(1)).encode("testPassword");
        verify(dao, times(1)).registerNewUser(any(CustomUser.class));
    }

    @Test
    void transferMoney() {
        TransferDto transfer = new TransferDto("testRecipientLogin", 10_000.00);
        when(dao.transferMoney(UserServiceTest.TEST_LOGIN, transfer.recipientLogin(), transfer.amount())).thenReturn(true);

        boolean isTransferSuccessful = service.transferMoney(UserServiceTest.TEST_LOGIN, transfer);

        assertTrue(isTransferSuccessful);
        verify(dao, times(1)).transferMoney(UserServiceTest.TEST_LOGIN, transfer.recipientLogin(), transfer.amount());
    }

    @Test
    void getBalance() {
        when(dao.getUserBalance(UserServiceTest.TEST_LOGIN)).thenReturn(Optional.of(anyDouble()));

        Optional<Double> balance = service.getBalance(UserServiceTest.TEST_LOGIN);

        assertTrue(balance.isPresent());
        verify(dao, times(1)).getUserBalance(UserServiceTest.TEST_LOGIN);
    }

    @Test
    void findUserByLogin() {
        when(dao.findUserByLogin(TEST_LOGIN)).thenReturn(Optional.of(new CustomUser(TEST_LOGIN, anyString(), Role.USER)));

        Optional<UserDetails> user = service.findUserByLogin(TEST_LOGIN);

        assertTrue(user.isPresent());
        verify(dao, times(1)).findUserByLogin(TEST_LOGIN);
    }
}