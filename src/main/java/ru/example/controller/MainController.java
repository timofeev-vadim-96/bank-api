package ru.example.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import ru.example.dto.TransferDto;
import ru.example.model.CustomUser;
import ru.example.service.UserService;
import ru.example.dto.AuthenticationRequest;
import ru.example.service.JwtService;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class MainController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @PostMapping("/signin")
    public ResponseEntity<String> authenticate(@RequestBody AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.login(), request.password()));
            final UserDetails user = userDetailsService.loadUserByUsername(request.login());
            if (user != null) {
                return new ResponseEntity<>(jwtService.generateToken(user), HttpStatus.OK);
            } else return ResponseEntity.status(401).body("Authentication failed");
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("Authentication failed");
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@RequestBody AuthenticationRequest request) {
        Optional<CustomUser> customUser = userService.registerNewUser(request);
        if (customUser.isPresent()) return new ResponseEntity<>(HttpStatus.CREATED);
        else return ResponseEntity.badRequest().build();
    }


    @PostMapping("/money")
    public ResponseEntity<Void> transferMoney(@RequestHeader("AUTHORIZATION") String authHeader,
                                              @RequestBody TransferDto transferDto) {
        String login = getLogin(authHeader);
        boolean isTransferSuccessful = userService.transferMoney(login, transferDto);
        if (isTransferSuccessful) return ResponseEntity.ok().build();
        else return ResponseEntity.badRequest().build();
    }

    @GetMapping("/money")
    public ResponseEntity<Double> getBalance(@RequestHeader("AUTHORIZATION") String authHeader) {
        //если мы попали сюда - то пользователь уже авторизован, в соответствии с SecurityFilterChain

        String login = getLogin(authHeader);
        Optional<Double> debitBalance = userService.getBalance(login);
        if (debitBalance.isEmpty()) return ResponseEntity.noContent().build();
        else return new ResponseEntity<>(debitBalance.get(), HttpStatus.OK);
    }

    private String getLogin(String authHeader) {
        String jwtToken = authHeader.substring(7);
        //достаем логин из jwt-токена
        return jwtService.extractUserName(jwtToken);
    }
}
