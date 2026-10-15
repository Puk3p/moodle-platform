-- Accounts and classes now live in identity-service's own database. Course data keeps their ids
-- as plain columns: new users and classes never appear in this database's users/classes tables,
-- so foreign keys into them would refuse every new enrollment, class assignment, teacher and
-- submission. What the keys did on deletion (remove enrollments and submissions, clear the
-- teacher) is now done on identity-service's identity.UserDeleted event (AccountHousekeeping).
--
-- The users, user_roles, user_sessions, classes, password_reset_tokens and profile_pictures tables
-- here are no longer used; deploy/data-moves/2026-10-move-accounts-to-identity.sql copies them
-- across. Drop them once the move has been checked.

ALTER TABLE courses DROP FOREIGN KEY fk_course_teacher;
ALTER TABLE course_classes DROP FOREIGN KEY fk_cc_class;
ALTER TABLE enrollments DROP FOREIGN KEY fk_enroll_user;
ALTER TABLE assignment_submissions DROP FOREIGN KEY fk_sub_student;
