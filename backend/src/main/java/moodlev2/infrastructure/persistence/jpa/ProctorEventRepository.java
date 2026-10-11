package moodlev2.infrastructure.persistence.jpa;

import java.util.List;
import moodlev2.infrastructure.persistence.jpa.entity.ProctorEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProctorEventRepository extends JpaRepository<ProctorEventEntity, Long> {

    List<ProctorEventEntity> findByAttemptIdOrderByOccurredAtAscIdAsc(Long attemptId);

    long countByAttemptId(Long attemptId);

    long countByAttemptIdAndEventTypeIn(Long attemptId, List<String> eventTypes);
}
