package moodlev2.identity.application.auth.interfaces;

import moodlev2.identity.domain.user.User;
import moodlev2.identity.web.auth.dto.RegisterRequest;

public interface IRegisterService {
    User register(RegisterRequest request);
}
