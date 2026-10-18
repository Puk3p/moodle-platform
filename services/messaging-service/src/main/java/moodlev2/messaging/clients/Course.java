package moodlev2.messaging.clients;

/**
 * A course, as academic-service's catalog describes it.
 *
 * @param teacherId the teaching account, {@code null} when none is assigned
 */
public record Course(long id, String code, String name, Long teacherId) {}
