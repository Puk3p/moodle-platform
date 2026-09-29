export interface RecentItem {
  title: string;
  score: string;
  percent: number;
  weightLabel: string | null;
  /** ISO-8601 date, or empty when unknown. */
  gradedOn: string;
  typeLabel: string;
  typeIcon: string;
}

export interface CourseGrade {
  code: string;
  name: string;
  term: string;
  instructor: string;
  /** Null until the course has a graded item. */
  gradeLetter: string | null;
  percentage: number | null;
  gradedCount: number;
  recentItems: RecentItem[];
}

export interface GradeBreakdown {
  totalCourses: number;
  aCourses: number;
  bCourses: number;
  cCourses: number;
  dCourses: number;
  fCourses: number;
  ungradedCourses: number;
}

export interface CourseHighlight {
  code: string;
  label: string;
}

export interface UpcomingGrade {
  courseCode: string;
  title: string;
  /** ISO-8601 date. */
  date: string;
  type: string;
}

export interface GradesPageResponse {
  courses: CourseGrade[];
  overallGpa: number | null;
  gradeBreakdown: GradeBreakdown;
  bestCourse: CourseHighlight | null;
  needsAttention: CourseHighlight | null;
  upcomingGradeReleases: UpcomingGrade[];
}
