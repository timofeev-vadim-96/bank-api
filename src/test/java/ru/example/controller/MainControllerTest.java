package ru.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.example.dto.AuthenticationRequest;
import ru.example.dto.TransferDto;
import ru.example.model.CustomUser;
import ru.example.util.Role;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Запускать с поднятой Postgres
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT) //для web test client
@AutoConfigureWebTestClient //для работы тестового веб-клиента
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //создает общий экземпляр класса для всех тестов
@Slf4j
class MainControllerTest {
    private static final String JWT_REGEX = "^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$";
    private static final String TEST_LOGIN = "testUser";
    private static final String TEST_PASSWORD = "testPassword";
    private static final double TEST_BALANCE = 15_000;

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private BeanPropertyRowMapper<CustomUser> userMapper;
    private volatile String jsonWebToken;

    @AfterAll
    static void clearDB(){
        JdbcTemplate jdbcTemplateAfter = new JdbcTemplate();
        jdbcTemplateAfter.setDataSource(new DriverManagerDataSource(
                "jdbc:postgresql://localhost:5432/bank_database",
                "postgres",
                "root"));

        jdbcTemplateAfter.update("delete from users where login = ?", TEST_LOGIN);
        jdbcTemplateAfter.update("delete from users where login = ?", "testLogin2");
    }

    @Test
    @Order(2)
    void authenticate() {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest(TEST_LOGIN, TEST_PASSWORD);

        jsonWebToken = webTestClient.post()
                .uri("/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authenticationRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(jsonWebToken);
        assertTrue(jsonWebToken.matches(JWT_REGEX));
    }

    @Test
    @Order(1)
    void signUp() {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest(TEST_LOGIN, TEST_PASSWORD);

        webTestClient.post()
                .uri("/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authenticationRequest)
                .exchange()
                .expectStatus().isCreated();
        CustomUser user = jdbcTemplate.queryForObject("select * from users where login = ?", userMapper, TEST_LOGIN);

        assertNotNull(user);
        assertEquals(TEST_LOGIN, user.getLogin());
    }

    @Test
    @Order(4)
    void transferMoney() {
        final double TEST_BALANCE_2 = 5_000;
        final String TEST_LOGIN_2 = "testLogin2";
        try {
            jdbcTemplate.update("insert into users (login, password, role, debit_balance) VALUES (?, ?, ?, ?)",
                    TEST_LOGIN_2, "testPassword2", Role.USER.toString(), TEST_BALANCE_2);
        } catch (DataAccessException e){
            log.info("user with login: {} already exists", TEST_BALANCE_2);
        }
        TransferDto transfer = new TransferDto(TEST_LOGIN_2, 2_333.494);

        webTestClient.post()
                .uri("/money")
                .header("AUTHORIZATION", "Bearer " + jsonWebToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(transfer)
                .exchange()
                .expectStatus().isOk();
        Double debitBalanceSender = jdbcTemplate.queryForObject("select debit_balance from users where login = ?", Double.class, TEST_LOGIN);
        Double debitBalanceRecipient = jdbcTemplate.queryForObject("select debit_balance from users where login = ?", Double.class, TEST_LOGIN_2);

        assertEquals(TEST_BALANCE - transfer.amount(), debitBalanceSender);
        assertEquals(TEST_BALANCE_2 + transfer.amount(), debitBalanceRecipient);
    }

    @Test
    @Order(3)
    void getBalance() {
        jdbcTemplate.update("update users set debit_balance = ? where login = ?", TEST_BALANCE, TEST_LOGIN);

        Double debitBalance = webTestClient.get()
                .uri("/money")
                .header("AUTHORIZATION", "Bearer " + jsonWebToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(debitBalance);
        assertEquals(TEST_BALANCE, debitBalance);
    }
}