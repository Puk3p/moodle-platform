export interface ActiveCourse {
  code: string;
  title: string;
  teacher: string;
  progress: number;
  accent: string;
}

export interface CompletedCourse {
  code: string;
  title: string;
  completedAt: string;
}

export interface Deadline {
  title: string;
  course: string;
  dueIn: string;
}

export interface Activity {
  text: string;
  timeAgo: string;
}

export interface DashboardHomeResponse {
  userName: string;
  activeCourses: ActiveCourse[];
  completedCourses: CompletedCourse[];
  deadlines: Deadline[];
  activities: Activity[];
}
