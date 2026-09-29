package moodlev2.application.user;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.SessionService;
import moodlev2.infrastructure.persistence.jpa.UserSessionRepository;
import moodlev2.web.user.dto.SessionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The "Login & devices" list. Revocations go through SessionService so live sockets close too. */
@Service
@RequiredArgsConstructor
public class ManageSessionsService {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("MMM dd, HH:mm").withZone(ZoneId.systemDefault());

    private final UserSessionRepository sessionRepository;
    private final SessionService sessionService;

    @Transactional(readOnly = true)
    public List<SessionDto> getUserSessions(String email, String currentSessionHash) {
        return sessionRepository.findAllByUserEmail(email).stream()
                .map(
                        s ->
                                new SessionDto(
                                        s.getId(),
                                        s.getDeviceName(),
                                        s.getIpAddress(),
                                        s.getLastActive() == null
                                                ? ""
                                                : FORMAT.format(s.getLastActive()),
                                        s.getTokenSignature().equals(currentSessionHash)))
                .toList();
    }

    public void revokeSession(Long sessionId, Long userId) {
        sessionService.revokeById(sessionId, userId);
    }

    public void revokeAllOtherSessions(Long userId, String currentSessionHash) {
        sessionService.revokeOthers(userId, currentSessionHash);
    }
}
