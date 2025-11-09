package moodlev2.domain.user.ports;

public interface PasswordHasherPort {
    String hash(String rawPassword); //primeste parola si intoarace hash
    boolean matches(String rawPassword, String hashedPassword); //verifica daca parola corespunde hashului
}
