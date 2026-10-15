package moodlev2.assessment.clients;

/** A course, as academic-service's catalog describes it. */
public record CourseSummary(long id, String code, String name, Long teacherId) {}
