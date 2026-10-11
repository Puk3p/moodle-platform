package moodlev2.progress.core;

/**
 * A course, as core's catalog describes it.
 *
 * @param teacherId the teaching account, {@code null} when none is assigned
 */
public record Course(long id, String code, String name, String term, Long teacherId) {}
