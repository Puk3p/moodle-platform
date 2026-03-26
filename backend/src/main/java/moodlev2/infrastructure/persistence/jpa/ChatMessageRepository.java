package moodlev2.infrastructure.persistence.jpa;

import java.util.List;
import moodlev2.infrastructure.persistence.jpa.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    @Query(
            "SELECT m FROM ChatMessageEntity m WHERE m.isPrivate = true AND (m.sender = :email OR m.recipient = :email) ORDER BY m.timestamp ASC")
    List<ChatMessageEntity> findChatHistory(@Param("email") String email);
}
