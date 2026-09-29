package moodlev2.infrastructure.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;
import moodlev2.infrastructure.persistence.jpa.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    @Query(
            "SELECT m FROM ChatMessageEntity m WHERE m.isPrivate = true AND (m.sender = :email OR m.recipient = :email) ORDER BY m.timestamp ASC")
    List<ChatMessageEntity> findChatHistory(@Param("email") String email);

    /** Everyone this user has exchanged a private message with. */
    @Query(
            "SELECT DISTINCT CASE WHEN m.sender = :email THEN m.recipient ELSE m.sender END "
                    + "FROM ChatMessageEntity m "
                    + "WHERE m.isPrivate = true AND (m.sender = :email OR m.recipient = :email)")
    List<String> findConversationPartners(@Param("email") String email);

    /**
     * Marks what {@code partner} sent to {@code me} as read, up to and including {@code upToId}.
     * The bound keeps a message that arrived after the client looked from being marked with it.
     */
    @Modifying
    @Query(
            "UPDATE ChatMessageEntity m SET m.readAt = :now "
                    + "WHERE m.isPrivate = true AND m.recipient = :me AND m.sender = :partner "
                    + "AND m.readAt IS NULL AND m.id <= :upToId")
    int markRead(
            @Param("me") String me,
            @Param("partner") String partner,
            @Param("upToId") long upToId,
            @Param("now") LocalDateTime now);

    // Messages reference people by email rather than by id, so these keep history consistent
    // when an account is deleted or its email changes: otherwise a new account registered with
    // the old address would inherit the conversations.

    /** Removes every message the user sent or received. */
    @Modifying
    @Query("DELETE FROM ChatMessageEntity m WHERE m.sender = :email OR m.recipient = :email")
    int deleteAllByParticipant(@Param("email") String email);

    @Modifying
    @Query("UPDATE ChatMessageEntity m SET m.sender = :newEmail WHERE m.sender = :oldEmail")
    int renameSender(@Param("oldEmail") String oldEmail, @Param("newEmail") String newEmail);

    @Modifying
    @Query("UPDATE ChatMessageEntity m SET m.recipient = :newEmail WHERE m.recipient = :oldEmail")
    int renameRecipient(@Param("oldEmail") String oldEmail, @Param("newEmail") String newEmail);
}
