package moodlev2.application.auth.interfaces;

import moodlev2.web.auth.dto.AuthResponse;
import moodlev2.web.auth.dto.LoginRequest;

public interface ILoginService {
    AuthResponse login(LoginRequest request);
}
