package ru.infinitesynergy.bankapi.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import ru.infinitesynergy.bankapi.util.Role;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomUser {
    @Id
    private Long id;
    @NonNull
    private String login;
    @NonNull
    private String password;
    @NonNull
    private Role role;
    private double debitBalance;

    public CustomUser(@NonNull String login, @NonNull String password, @NonNull Role role) {
        this.login = login;
        this.password = password;
        this.role = role;
        debitBalance = 0;
    }

    public CustomUser(@NonNull String login, @NonNull String password, @NonNull Role role, double debitBalance) {
        this.login = login;
        this.password = password;
        this.role = role;
        this.debitBalance = debitBalance;
    }
}
