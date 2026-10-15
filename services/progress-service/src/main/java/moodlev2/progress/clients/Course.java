package moodlev2.progress.clients;

/**
 * A course, as academic-service's catalog describes it.
 *
 * @param teacherId the teaching account, {@code null} when none is assigned
 */
public record Course(long id, String code, String name, String term, Long teacherId) {}
