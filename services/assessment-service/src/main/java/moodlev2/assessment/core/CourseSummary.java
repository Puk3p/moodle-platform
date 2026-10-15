package moodlev2.assessment.core;

/** A course, as core's catalog describes it. */
public record CourseSummary(long id, String code, String name, Long teacherId) {}
