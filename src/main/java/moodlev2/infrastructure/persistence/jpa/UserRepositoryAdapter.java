package moodlev2.infrastructure.persistence.jpa;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("mysql")
@RequiredArgsConstructor
public class UserRepositoryAdapter{
}
