package ru.infinitesynergy.bankapi.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import ru.infinitesynergy.bankapi.dto.AuthenticationRequest;
import ru.infinitesynergy.bankapi.service.UserService;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer {
    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;

    @Value("${application.admin.login}")
    private String adminLogin;
    @Value("${application.admin.password}")
    private String adminPassword;

    @EventListener(ContextRefreshedEvent.class)
    private void initialize() {
        createDatabase();
        createTable();
        createTestUsers();
    }

    private void createDatabase() {
        try {
            jdbcTemplate.execute("CREATE DATABASE bank_database");
        } catch (DataAccessException e) {
            log.warn("Exception while trying to create a database. Perhaps it already exists ");
        } finally {
            jdbcTemplate.setDataSource(new DriverManagerDataSource(
                    "jdbc:postgresql://localhost:5432/bank_database",
                    "postgres",
                    "root"));
        }
    }

    private void createTable() {
        try {
            jdbcTemplate.execute("create table users " +
                    "(id bigserial primary key," +
                    "login varchar unique not null," +
                    "password varchar not null," +
                    "role varchar not null," +
                    "debit_balance double precision not null" +
                    ")");
        } catch (DataAccessException e) {
            log.warn("Exception while trying to create a table. Perhaps it already exists ");
        }
    }


    private void createTestUsers() {
        createUser(adminLogin, adminPassword, Role.ADMIN, 500_000);
        createUser("user1", "user1password", Role.USER, 250_000);
        createUser("user2", "user2password", Role.USER, 150_000);
    }

    private void createUser(String login, String password, Role role, double debitAmount) {
        try {
            userService.registerNewUser(new AuthenticationRequest(login, password), role, debitAmount);
        } catch (DataAccessException e) {
            log.warn("Exception while trying to initialize a/an {} to DB. Perhaps it already exists", login);
        }
    }
}
