export type ProfileRole = 'STUDENT' | 'TEACHER' | 'ADMIN';

/**
 * The signed-in user's own account. Exactly one of `student` / `teacher` is set for those roles,
 * and neither for an admin-only account.
 */
export interface UserProfile {
  email: string;
  firstName: string;
  lastName: string;
  role: ProfileRole;
  twoFaEnabled: boolean;
  student: StudentProfile | null;
  teacher: TeacherProfile | null;
}

export interface StudentProfile {
  studentId: string;
  /** Null until the student is placed in a class. */
  className: string | null;
  courses: EnrolledCourse[];
}

export interface EnrolledCourse {
  code: string;
  name: string;
  term: string;
  teacherName: string | null;
}

export interface TeacherProfile {
  courses: TaughtCourse[];
  /** Distinct students across every taught course. */
  studentCount: number;
}

export interface TaughtCourse {
  code: string;
  name: string;
  term: string;
  status: string;
  studentCount: number;
}
