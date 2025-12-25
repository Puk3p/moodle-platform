export interface TeacherActivity {
  type: 'submission' | 'resource' | 'announcement';
  courseCode: string;
  title: string;
  subtitle: string;
  timeAgo: string;
  icon: string;
}

export interface TeacherCourse {
  code: string;
  title: string;
  studentsCount: number;
  modulesCount: number;
  term: string;
  status: 'Published' | 'Draft';
  avgGrade?: number;
  pendingSubmissions?: number;
  isStarted: boolean;
}

export interface TeacherDashboardResponse {
  courses: TeacherCourse[];
  recentActivities: TeacherActivity[];
}