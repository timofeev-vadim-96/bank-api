package ru.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.example.dao.UserRepository;
import ru.example.dto.TransferDto;
import ru.example.dto.AuthenticationRequest;
import ru.example.model.CustomUser;
import ru.example.util.Role;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userDao;
    private final PasswordEncoder passwordEncoder;

    public Optional<CustomUser> registerNewUser(AuthenticationRequest authenticationRequest, Role role, double debitBalance) {
        CustomUser customUser = CustomUser.builder()
                .login(authenticationRequest.login())
                .password(passwordEncoder.encode(authenticationRequest.password()))
                .role(role)
                .debitBalance(debitBalance)
                .build();
        return userDao.registerNewUser(customUser);
    }

    public Optional<CustomUser> registerNewUser(AuthenticationRequest authenticationRequest) {
        return registerNewUser(authenticationRequest, Role.USER, 0);
    }

    public boolean transferMoney(String from, TransferDto transferDto) {
        return userDao.transferMoney(from, transferDto.recipientLogin(), transferDto.amount());
    }

    public Optional<Double> getBalance(String login) {
        return userDao.getUserBalance(login);
    }

    public Optional<UserDetails> findUserByLogin(String login) {
        Optional<CustomUser> userOptional = userDao.findUserByLogin(login);
        if (userOptional.isEmpty()) return Optional.empty();
        else {
            CustomUser user = userOptional.get();
            UserDetails userDetails = new User(
                    user.getLogin(),
                    user.getPassword(),
                    Collections.singleton(new SimpleGrantedAuthority(user.getRole().toString())));
            return Optional.of(userDetails);
        }
    }
}
