package moodlev2.academic.api;

/**
 * A teacher removed a module from a course. Published inside the editing transaction; quizzes filed
 * under it (assessment-service) become unfiled, as the old foreign key's ON DELETE SET NULL did.
 */
public record ModuleRemovedEvent(long courseId, long moduleId) {}
