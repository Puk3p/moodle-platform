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
}