package moodlev2.application.auth.interfaces;

import moodlev2.web.auth.dto.AuthResponse;
import moodlev2.web.auth.dto.RegisterRequest;

public interface IRegisterService {
    AuthResponse register(RegisterRequest request);
}
