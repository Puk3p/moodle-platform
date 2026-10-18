package moodlev2.assessment.infrastructure.persistence;

import java.util.List;
import moodlev2.assessment.infrastructure.persistence.entity.ProctorEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProctorEventRepository extends JpaRepository<ProctorEventEntity, Long> {

    List<ProctorEventEntity> findByAttemptIdOrderByOccurredAtAscIdAsc(Long attemptId);

    long countByAttemptId(Long attemptId);

    long countByAttemptIdAndEventTypeIn(Long attemptId, List<String> eventTypes);
}
