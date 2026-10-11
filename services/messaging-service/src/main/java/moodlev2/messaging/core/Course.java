package moodlev2.messaging.core;

/**
 * A course, as core's catalog describes it.
 *
 * @param teacherId the teaching account, {@code null} when none is assigned
 */
public record Course(long id, String code, String name, Long teacherId) {}
