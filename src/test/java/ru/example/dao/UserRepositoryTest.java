package ru.example.dao;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import ru.example.model.CustomUser;
import ru.example.util.Role;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Запускать с поднятой Postgres
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //создает общий экземпляр класса для всех тестов
@Slf4j
class UserRepositoryTest {
    @Autowired
    private UserRepository dao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String TEST_LOGIN = "testLogin";
    private static final String TEST_PASSWORD = "encodedPassword";
    private static final double TEST_BALANCE = 15_000.0;

    @AfterAll
    static void clearDB() {
        JdbcTemplate jdbcTemplateAfter = new JdbcTemplate();
        jdbcTemplateAfter.setDataSource(new DriverManagerDataSource(
                "jdbc:postgresql://localhost:5432/bank_database",
                "postgres",
                "root"));

        jdbcTemplateAfter.update("delete from users where login = ?", TEST_LOGIN);
        jdbcTemplateAfter.update("delete from users where login = ?", "testLogin2");
    }

    @Test
    @Order(1)
    void registerNewUser() {
        CustomUser user = CustomUser.builder()
                .login(TEST_LOGIN)
                .password(TEST_PASSWORD)
                .debitBalance(TEST_BALANCE)
                .role(Role.USER)
                .build();

        Optional<CustomUser> registeredUser = dao.registerNewUser(user);

        assertTrue(registeredUser.isPresent());
        assertEquals(TEST_LOGIN, registeredUser.get().getLogin());
        assertEquals(TEST_PASSWORD, registeredUser.get().getPassword());
        assertEquals(TEST_BALANCE, registeredUser.get().getDebitBalance());
        assertEquals(Role.USER, registeredUser.get().getRole());
        assertNotNull(registeredUser.get().getId());
    }

    @Test
    @Order(2)
    void findUserByLogin() {
        Optional<CustomUser> userByLogin = dao.findUserByLogin(TEST_LOGIN);

        assertTrue(userByLogin.isPresent());
        assertEquals(TEST_LOGIN, userByLogin.get().getLogin());
    }

    @Test
    @Order(4)
    void transferMoney() {
        final double TEST_BALANCE_2 = 5_000;
        final String TEST_LOGIN_2 = "testLogin2";
        try {
            jdbcTemplate.update("insert into users (login, password, role, debit_balance) VALUES (?, ?, ?, ?)",
                    TEST_LOGIN_2, "testPassword2", Role.USER.toString(), TEST_BALANCE_2);
        } catch (DataAccessException e) {
            log.info("user with login: {} already exists", TEST_BALANCE_2);
        }
        double transferAmount =  2_333.494;

        boolean isTransferSuccessful = dao.transferMoney(TEST_LOGIN, TEST_LOGIN_2, transferAmount);
        Double debitBalanceSender = jdbcTemplate.queryForObject("select debit_balance from users where login = ?", Double.class, TEST_LOGIN);
        Double debitBalanceRecipient = jdbcTemplate.queryForObject("select debit_balance from users where login = ?", Double.class, TEST_LOGIN_2);

        assertTrue(isTransferSuccessful);
        assertEquals(TEST_BALANCE - transferAmount, debitBalanceSender);
        assertEquals(TEST_BALANCE_2 + transferAmount, debitBalanceRecipient);
    }

    @Test
    @Order(3)
    void getUserBalance() {
        Optional<Double> userBalance = dao.getUserBalance(TEST_LOGIN);

        assertTrue(userBalance.isPresent());
        assertEquals(TEST_BALANCE, userBalance.get());
    }
}