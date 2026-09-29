package moodlev2.application.auth.interfaces;

import moodlev2.domain.user.User;
import moodlev2.web.auth.dto.RegisterRequest;

public interface IRegisterService {
    User register(RegisterRequest request);
}
