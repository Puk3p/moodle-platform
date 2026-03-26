package moodlev2.application.user;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.infrastructure.persistence.jpa.UserSessionRepository;
import moodlev2.web.user.dto.SessionDto;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManageSessionsService {

    private final UserSessionRepository sessionRepository;

    public List<SessionDto> getUserSessions(String email, String currentToken) {
        String currentSignature = currentToken.substring(currentToken.length() - 15);
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMM dd, HH:mm").withZone(ZoneId.systemDefault());

        return sessionRepository.findAllByUserEmail(email).stream()
                .map(
                        s ->
                                new SessionDto(
                                        s.getId(),
                                        s.getDeviceName(),
                                        s.getIpAddress(),
                                        formatter.format(s.getLastActive()),
                                        s.getTokenSignature().equals(currentSignature)))
                .toList();
    }

    public void revokeSession(Long sessionId, String email) {
        sessionRepository.deleteByIdAndUserEmail(sessionId, email);
    }

    public void revokeAllOtherSessions(String email, String currentToken) {
        String currentSignature = currentToken.substring(currentToken.length() - 15);
        sessionRepository.deleteByUserEmailAndTokenSignatureNot(email, currentSignature);
    }
}
