import { Activity, Deadline } from './dashboard.model';

export interface CourseOverview {
  id: string;
  code: string;
  title: string;
  prof: string;
  progress: number;
  nextDeadline: string;
  imageUrl: string;
}

export interface CompletedCourseSummary {
  title: string;
  date: string;
  grade: string;
}

export interface CoursesPageResponse {
  userName: string;
  userRole: string;
  userAvatarUrl: string;
  activeCourses: CourseOverview[];
  completedCourses: CompletedCourseSummary[];
  /** The soonest upcoming deadlines across the student's courses (same as the dashboard's). */
  deadlines: Deadline[];
  /** The latest announcements in them, newest first. */
  activities: Activity[];
}