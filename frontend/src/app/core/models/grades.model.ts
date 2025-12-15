export type CourseStatus = 'in-progress' | 'completed';

export interface RecentItem {
  title: string;
  score: string;
  percent: number;
  weightLabel: string;
  gradedOn: string;
  typeLabel: string;
  typeIcon: string;
}

export interface CourseGrade {
  code: string;
  name: string;
  instructor: string;
  gradeLetter: string;
  percentage: number;
  status: CourseStatus;
  isCurrent: boolean;
  recentItems?: RecentItem[];
}

export interface GradesPageResponse {
  courses: CourseGrade[];
  overallGpa: number;
  gpaDelta: number;
  gradeBreakdown: {
    totalCourses: number;
    aCourses: number;
    bCourses: number;
    cCourses: number;
  };
  bestCourse: { code: string; label: string };
  needsAttention: { code: string; label: string };
  upcomingGradeReleases: { course: string; dateLabel: string }[];
}