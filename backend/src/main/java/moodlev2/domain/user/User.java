package moodlev2.domain.user;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Getter @Setter
public class User {
    private Long id; //id pt utiliz
    private String email; //email pt login/reg
    private String passwordHash; //hash parola
    private String firstName; //prenume
    private String lastName; //nume

    private Set<Role> roles = EnumSet.noneOf(Role.class); //roluri utilizator

    private boolean enabled = true; //daca contul e activ se logheaza(bypass)

    private Instant createdAt; //data creare cont
    private Instant updatedAt; //data ultima actualizare cont

    private Long classId;


    private String twoFaSecret;
    private boolean twoFaEnabled;

    //metoda pentru verificare roluri gen
    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }
}
