package ru.infinitesynergy.bankapi.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.infinitesynergy.bankapi.model.CustomUser;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j(topic = "bank-api-log")
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;
    private final BeanPropertyRowMapper<CustomUser> userMapper;

    public Optional<CustomUser> registerNewUser(CustomUser user) {
        try {
            Optional<CustomUser> customUser = findUserByLogin(user.getLogin());
            if (customUser.isEmpty()) {
                jdbcTemplate.update("insert into users (login, password, role, debit_balance) values (?, ?, ?, ?)",
                        user.getLogin(), user.getPassword(), user.getRole().toString(), user.getDebitBalance());
                return Optional.ofNullable(jdbcTemplate.queryForObject("select * from users where login = ?",
                        userMapper, user.getLogin()));
            }
            else return Optional.empty();
        } catch (DataAccessException e) {
            log.error("An exception has occurred while trying to register user: {}. An exception: ", user, e);
            return Optional.empty();
        }
    }

    public Optional<CustomUser> findUserByLogin(String login) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("select * from users where login = ?",
                    userMapper, login));
        } catch (DataAccessException e) {
            log.warn("User with login: {}, not found.", login);
            return Optional.empty();
        }
    }

    @Transactional
    public boolean transferMoney(String loginFrom, String loginTo, double amount){
        try{
            Double currentBalanceSender = jdbcTemplate.queryForObject("select debit_balance from users where login = ?",
                    Double.class, loginFrom);
            if (currentBalanceSender == null || currentBalanceSender < amount ) return false;
            Double currentBalanceRecipient = jdbcTemplate.queryForObject("select debit_balance from users where login = ?",
                    Double.class, loginTo);
            if (currentBalanceRecipient == null) return false;
            jdbcTemplate.update("update users set debit_balance = ? where login = ?", currentBalanceSender - amount, loginFrom);
            jdbcTemplate.update("update users set debit_balance = ? where login = ?", currentBalanceRecipient + amount, loginTo);
            return true;
        } catch (DataAccessException e){
            log.error("Exception while trying to transfer money from " + loginFrom + " to " + loginTo);
            return false;
        }
    }

    public Optional<Double> getUserBalance(String login){
        try{
            Double userBalance = jdbcTemplate.queryForObject("select debit_balance from users where login = ?",
                    Double.class, login);
            return Optional.ofNullable(userBalance);
        } catch (DataAccessException e) {
            log.warn("User with login: {}, not found.", login);
            return Optional.empty();
        }
    }
}
